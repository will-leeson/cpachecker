from subprocess import Popen, PIPE
from sys import argv
import numpy as np
from lib.gnn import GAT
import torch
from torch_geometric.data import Data, Batch

tokenDict = {"--": 0, "const unsigned int": 1, "const signed char": 2, "/": 3, "<<": 4, "void": 5, "IntegerLiteral": 6, "array": 7, "input u16 ": 8, "const long long": 9, "TranslationUnit": 10, "input char ": 11, "const long": 12, "input void * ": 13, "||": 14, "Empty": 15, "ContinueStmt": 16, "|=": 17, "input u8 ": 18, "struct": 19, "&&": 20, "long double": 21, "%": 22, "volatile char": 23, "BinaryOperator": 24, "CompoundStmt": 25, "const float": 26, "IfStmt": 27, "input sector_t ": 28, "const char": 29, "OpaqueValueExpr": 30, "volatile unsigned short": 31, "float": 32, "input loff_t ": 33, "-=": 34, "<<=": 35, "^=": 36, "=": 37, "const unsigned long": 38, "signed char": 39, "BreakStmt": 40, "input long long ": 41, "input unsigned long ": 42, "const _Bool": 43, "ConditionalOperator": 44, "input double ": 45, "Var": 46, "long long": 47, ">>": 48, "const unsigned short": 49, "unsigned long long": 50, ">=": 51, "*": 52, ">": 53, "SwitchStmt": 54, "^": 55, "UnaryExprOrTypeTraitExpr": 56, "input _Bool ": 57, "&": 58, "volatile _Bool": 59, "!": 60, "DesignatedInitExpr": 61, "volatile unsigned long": 62, "FloatingLiteral": 63, "input pthread_t ": 64, "input const char * ": 65, "LabelStmt": 66, "const int": 67, "!=": 68, "<": 69, "CompoundLiteralExpr": 70, "int": 71, "input size_t ": 72, "ForStmt": 73, "_Bool": 74, "GotoStmt": 75, "main": 76, "ParmVar": 77, "Field": 78, "ReturnStmt": 79, "long": 80, "char": 81, "EnumConstant": 82, "ImplicitCastExpr": 83, "OffsetOfExpr": 84, "PredefinedExpr": 85, "UnaryOperator": 86, "volatile int": 87, "otherType": 88, "volatile unsigned long long": 89, "unsigned long": 90, "DeclRefExpr": 91, "input unsigned int ": 92, "<=": 93, "WhileStmt": 94, "const double": 95, "Typedef": 96, "|": 97, "DefaultStmt": 98, "input float ": 99, "+=": 100, "CommaOperator": 101, "~": 102, "InitListExpr": 103, "input long ": 104, "input uint ": 105, "CharacterLiteral": 106, "==": 107, "unsigned short": 108, "CallExpr": 109, "volatile long": 110, "Record": 111, "++": 112, "input short ": 113, "VAArgExpr": 114, "const unsigned long long": 115, "double": 116, "volatile unsigned int": 117, "DeclStmt": 118, "*=": 119, "Function": 120, "input char * ": 121, "DoStmt": 122, "StmtExpr": 123, "short": 124, "Enum": 125, "ParenExpr": 126, "+": 127, "TypeTraitExpr": 128, "&=": 129, "MemberExpr": 130, "ConstantExpr": 131, "input unsigned long long ": 132, "CStyleCastExpr": 133, "pointer": 134, "CaseStmt": 135, "unsigned char": 136, "input int ": 137, "-": 138, "input unsigned char ": 139, "input unsigned short ": 140, "/=": 141, "input u32 ": 142, ">>=": 143, "StringLiteral": 144, "ChooseExpr": 145, "input ulong ": 146, "const unsigned char": 147, "ArraySubscriptExpr": 148, "NullStmt": 149, "const short": 150, "GCCAsmStmt": 151, "BinaryConditionalOperator": 152, "unsigned int": 153, "volatile unsigned char": 154}

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

graphBuilder = Popen(['graph-builder', argv[1]], stdout=PIPE, stderr=PIPE)
stdout, stderr = graphBuilder.communicate()
stdout = stdout.decode("utf-8")
graph = stdout.split('\n')
graph = [x for x in graph if x != '']

graph =  handler(graph)
nodes, ast, cfg, dfg = makeFinalRep(graph)


possible = ["PA", "KI", "VA-NoCegar", "BMC", "VA-Cegar", "Unknown", ]

nodes = torch.from_numpy(nodes)

edges_tensor = [torch.from_numpy(edgeSet) for edgeSet in [ast, cfg, dfg]]

edge_labels = torch.cat([torch.full((len(edges_tensor[i]),1),i) for i in range(len(edges_tensor))], dim=0)        
edges_tensor = torch.cat(edges_tensor).transpose(0,1).long()

data = Data(x=nodes.float(), edge_index=edges_tensor, edge_attr=edge_labels, problemType=torch.FloatTensor([1]))

data = Batch.from_data_list([data])

model = GAT(passes=1, numEdgeSets=3, inputLayerSize=nodes.size(1), outputLayerSize=len(possible), numAttentionLayers=5, mode='cat', pool='mean', k='3')
model.load_state_dict(torch.load(argv[2]))

prediction = (-model(x=data.x, edge_index=data.edge_index, edge_attr=data.edge_attr, problemType=data.problemType, batch=data.batch)).argsort()

order = [possible[x] for x in prediction.squeeze()]

for analysis in order:
    print(analysis)