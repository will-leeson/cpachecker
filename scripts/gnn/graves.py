from subprocess import Popen, PIPE
from sys import argv
import numpy as np
from lib.gnn import EGC
import torch
from torch_geometric.data import Data, Batch

tokenDict = {"input short ": 0, "SwitchStmt": 1, "volatile _Bool": 2, "BinaryOperator": 3, "const int": 4, "DefaultStmt": 5, "input u16 ": 6, "input unsigned long ": 7, "float": 8, "/=": 9, "+=": 10, "input long long ": 11, "<=": 12, "void": 13, "volatile unsigned char": 14, "CaseStmt": 15, "input unsigned short ": 16, "input float ": 17, "const double": 18, "int": 19, "=": 20, "ParenExpr": 21, "Enum": 22, "DeclRefExpr": 23, "const _Bool": 24, "ForStmt": 25, "StringLiteral": 26, "<<=": 27, "^=": 28, "input int ": 29, "IntegerLiteral": 30, "OpaqueValueExpr": 31, "short": 32, "IfStmt": 33, "Typedef": 34, "input bool ": 35, "long": 36, "const unsigned int": 37, "ConstantExpr": 38, "input char * ": 39, "DeclStmt": 40, "struct": 41, "signed char": 42, "InitListExpr": 43, "long double": 44, "ReturnStmt": 45, "WhileStmt": 46, "!=": 47, "input unsigned int ": 48, "_Bool": 49, "input _Bool ": 50, "input ulong ": 51, "LabelStmt": 52, "*=": 53, "const unsigned long long": 54, "input unsigned long long ": 55, "<<": 56, ">": 57, "input u32 ": 58, "BreakStmt": 59, "otherType": 60, "+": 61, "const unsigned short": 62, "const float": 63, "&=": 64, "ParmVar": 65, "ImplicitCastExpr": 66, "Field": 67, "const long": 68, "--": 69, "Empty": 70, "-": 71, "const short": 72, "TypeTraitExpr": 73, "GCCAsmStmt": 74, "input loff_t ": 75, "~": 76, "array": 77, "DesignatedInitExpr": 78, "AtomicExpr": 79, "CompoundStmt": 80, "CallExpr": 81, "__extension__": 82, "unsigned long long": 83, "CommaOperator": 84, "input uint ": 85, "UnaryExprOrTypeTraitExpr": 86, "unsigned int": 87, "|=": 88, "CharacterLiteral": 89, "PredefinedExpr": 90, "TranslationUnit": 91, "char": 92, "EnumConstant": 93, "Function": 94, "unsigned short": 95, "long long": 96, "==": 97, "volatile unsigned long long": 98, "%": 99, "|": 100, ">>=": 101, "volatile int": 102, "input const char * ": 103, "Var": 104, "&": 105, "CStyleCastExpr": 106, "unsigned long": 107, "++": 108, "DoStmt": 109, "^": 110, "input long ": 111, "input pthread_t ": 112, "MemberExpr": 113, "!": 114, "volatile unsigned long": 115, "double": 116, "volatile unsigned short": 117, "ContinueStmt": 118, "main": 119, "-=": 120, "const unsigned char": 121, ">=": 122, ">>": 123, "const unsigned long": 124, "CompoundLiteralExpr": 125, "pointer": 126, "Record": 127, "input sector_t ": 128, "/": 129, "StmtExpr": 130, "||": 131, "const char": 132, "GotoStmt": 133, "const long long": 134, "UnaryOperator": 135, "ConditionalOperator": 136, "ChooseExpr": 137, "ArraySubscriptExpr": 138, "input size_t ": 139, "volatile char": 140, "input u8 ": 141, "<": 142, "OffsetOfExpr": 143, "BinaryConditionalOperator": 144, "input char ": 145, "volatile unsigned int": 146, "input unsigned char ": 147, "volatile long": 148, "VAArgExpr": 149, "NullStmt": 150, "*": 151, "const signed char": 152, "&&": 153, "FloatingLiteral": 154, "unsigned char": 155, "input double ": 156}

def reachableDefs(cfgDict, genKillDict, stmtToNum, backwardsCFGDict, start):
    reachDefs = dict()
    for key in stmtToNum:
        for item in stmtToNum[key]:
            reachDefs[item] = dict()

    previousReachDef = []
    for _ in range(8):
        frontier = [start]
        visited = []
        while frontier:
            curr = frontier.pop()
            if curr not in cfgDict or curr not in backwardsCFGDict:
                continue
            
            gen_kill = set()
            stmtNums = stmtToNum[curr]
            for num in stmtNums:
                if num in genKillDict:
                    for item in genKillDict[num]:
                        gen_kill.add(item)
            
            inSet = dict()
            for node in backwardsCFGDict[curr]:
                for num in stmtToNum[node]:
                    for var in reachDefs[num]:
                        if var in inSet:
                            inSet[var].union(reachDefs[num][var])
                        else:
                            inSet[var] = reachDefs[num][var]
            
            outSet = inSet

            for item in gen_kill:
                newSet = set()
                newSet.add(item[0])
                outSet[item[1]] = newSet

            for item in cfgDict[curr]:
                if item not in visited:
                    frontier.append(item)
            visited.append(curr)

            for num in stmtNums:
                reachDefs[num] = outSet
        
        if previousReachDef == reachDefs:
            break
        else:
            previousReachDef = reachDefs

    return reachDefs

def defToRef(reachSet, refs):
    defToRefDict = dict()
    for ref in refs:
        reachable = reachSet[ref]
        if reachable:
            for var in refs[ref]:
                # print(var)
                # print(reachable)
                if var[1] in reachable:
                    for item in reachable[var[1]]:
                        if item in defToRefDict:
                            defToRefDict[item].append(var[0])
                        else:
                            defToRefDict[item] = [var[0]]
    return defToRefDict

def handler(rawGraph):
    ptrToToken = dict()
    astDict = dict()
    cfgDict = dict()
    backwardsCFGDict = dict()
    dfgDict = dict()
    genKillDict = dict()
    refDict = dict()
    stmtToNum = dict()
    tokenSet = set()
    start = None

    holdOnTo = None
    try:
        for line in rawGraph:
            if "(void)" in line:
                line = "".join(line.split("(void)"))
            newline = "".join(line.strip().split(")"))
            newline = "".join(newline.split("("))
            newline = newline.split(",")

            if newline[0] == "AST":
                #ASTPointer : ASTToken
                if newline[1] not in ptrToToken:
                    ptrToToken[newline[1]] = newline[2]
                if newline[3] not in ptrToToken:
                    ptrToToken[newline[3]] = newline[4]
                #ASTPointer1 : [ASTPointer2, ...]
                if newline[1] in astDict:
                    astDict[newline[1]].append(newline[3])
                else:
                    astDict[newline[1]] = [newline[3]]
            elif newline[0] == "CFG":
                #StatementPointer : StatementPointerSuccessor
                if newline[2] in cfgDict:
                    cfgDict[newline[2]].append(newline[4])
                else:
                    cfgDict[newline[2]] = [newline[4]]
                if ptrToToken[newline[4]] == "Function":
                    holdOnTo = newline[2]
                #The purpose of the backwardsCFGDict is to be able to build the inset
                #for reaching definitions. Since function calls will not add to this
                #set and only serve to cause problems, we're ditching them
                elif holdOnTo:
                    if newline[4] in backwardsCFGDict:
                        backwardsCFGDict[newline[4]].append(holdOnTo)
                    else:
                        backwardsCFGDict[newline[4]] = [holdOnTo]
                    holdOnTo = None
                else:
                    if newline[4] in backwardsCFGDict:
                        backwardsCFGDict[newline[4]].append(newline[2])
                    else:
                        backwardsCFGDict[newline[4]] = [newline[2]]
                if newline[2] in stmtToNum:
                    stmtToNum[newline[2]].append(newline[1])
                else:
                    stmtToNum[newline[2]] = [newline[1]]
                if newline[3] == "main":
                    start = newline[4]

            elif newline[0] == "DFG":
                #DataPoint : DataPointAcceptor
                if newline[1] in dfgDict:
                    dfgDict[newline[1]].append(newline[3])
                else:
                    dfgDict[newline[1]] = [newline[3]]
            elif newline[0] == "Gen/Kill":
                # CFGNum : (DeclRefExpr, Var)
                if newline[1] in genKillDict:
                    genKillDict[newline[1]].append((newline[2],newline[3]))
                else:
                    genKillDict[newline[1]] = [(newline[2],newline[3])]
            elif newline[0] == "Ref":
                #CFGNum : (DeclRefExpr, Var)
                if newline[1] in refDict:
                    refDict[newline[1]].append((newline[3],newline[4]))
                else:
                    refDict[newline[1]] = [(newline[3],newline[4])]
    except:
        print(line)
        assert()

    reachDef = reachableDefs(cfgDict, genKillDict, stmtToNum, backwardsCFGDict, start)
    defsToRefs = defToRef(reachDef, refDict)
    for aDef in defsToRefs:
        if aDef in dfgDict:
            dfgDict[aDef].append(defsToRefs[aDef])
        else:
            dfgDict[aDef] = [defsToRefs[aDef]]

    output = {"tokens":ptrToToken, "AST":astDict, "CFG":cfgDict, "DFG":dfgDict}
    return output

def makeFinalRep(graphDict):
    nodeRepresentations = []
    counter = 0
    tokenToNum = dict()
    for token in graphDict["tokens"]:
        if token in tokenToNum:
            continue
        else:
            tokenToNum[token] = counter
            counter+=1
        aList = np.array([0]*len(tokenDict))
        if graphDict["tokens"][token] in tokenDict:
            aList[tokenDict[graphDict["tokens"][token]]] = 1
        else:
            aList[-1] = 1
        nodeRepresentations.append(aList)
    
    ASTDict = []
    for outNode in graphDict["AST"]:
        for inNode in graphDict["AST"][outNode]:
            assert(outNode in tokenToNum)
            assert(inNode in tokenToNum)
            ASTDict.append([tokenToNum[outNode],tokenToNum[inNode]])

    CFGDict = []
    for outNode in graphDict["CFG"]:
        for inNode in graphDict["CFG"][outNode]:
            if outNode not in tokenToNum:
                print(graph)
                print(graphDict['CFG'][outNode])
                print(outNode)
                assert()
            assert(inNode in tokenToNum)
            CFGDict.append([tokenToNum[outNode],tokenToNum[inNode]])
            
    DFGDict = []
    try:
        for outNode in graphDict["DFG"]:
            for inNode in graphDict["DFG"][outNode]:
                if type(inNode) == list:
                    for node in inNode:
                        DFGDict.append([tokenToNum[outNode],tokenToNum[node]])
                else:
                    if inNode not in tokenToNum:
                        continue
                    if outNode not in tokenToNum:
                        continue
                    DFGDict.append([tokenToNum[outNode],tokenToNum[inNode]])
    except KeyError:
        print(graph)
    nodeRepresentations = np.array(nodeRepresentations)
    
    AST = np.array(ASTDict)
    CFG = np.array(CFGDict)
    DFG = np.array(DFGDict)
    return nodeRepresentations, AST, CFG, DFG

#Call graph-builder and collect output
graphBuilder = Popen([argv[3], argv[1]], stdout=PIPE, stderr=PIPE)
stdout, stderr = graphBuilder.communicate()
stdout = stdout.decode("utf-8")
graph = stdout.split('\n')
graph = [x for x in graph if x != '']

#Convert graph into edge sets and node set
graph =  handler(graph)
nodes, ast, cfg, dfg = makeFinalRep(graph)

#Possible results
possible = ["BMC", "KI", "PA", "SymEx", "VA-NoCegar", "VA-Cegar"]

nodes = torch.from_numpy(nodes)

edges_tensor = [torch.from_numpy(edgeSet) for edgeSet in [ast]]
edges_tensor = edges_tensor[:1]

edge_labels = torch.cat([torch.full((len(edges_tensor[i]),1),i) for i in range(len(edges_tensor))], dim=0).float()  
edges_tensor = torch.cat(edges_tensor).transpose(0,1).long()

data = Data(x=nodes.float(), edge_index=edges_tensor, edge_attr=edge_labels, problemType=torch.FloatTensor([0]))

data = Batch.from_data_list([data])

#Build model and load weights
model = EGC(passes=1, inputLayerSize=nodes.size(1), outputLayerSize=len(possible), pool=['max','mean','attention'], aggregators=['max','mean','std'])
model.load_state_dict(torch.load(argv[2], map_location="cpu"))

#Make prediction
prediction = -model(x=data.x, edge_index=data.edge_index, problemType=data.problemType, batch=data.batch)
prediction = prediction.argsort()

order = [possible[x] for x in prediction.squeeze()]

for analysis in order:
    print(analysis)