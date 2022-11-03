import torch
import torch.nn as nn
import torch.nn.functional as f
from torch_geometric.nn import EGConv, JumpingKnowledge, MaxAggregation, MeanAggregation, SumAggregation, AttentionalAggregation, EquilibriumAggregation, MultiAggregation, PowerMeanAggregation, SoftmaxAggregation, MinAggregation
'''
File - ggnn.py
This file includes three architectures for GGNNs, two of which do not
actually use gated recurrent units.
'''

def build_aggregators(aggregator_type, fcInputLayerSize=None):
    if aggregator_type == "add":
        aggregator = SumAggregation()
    elif aggregator_type == "mean":
        aggregator = MeanAggregation()
    elif aggregator_type == "max":
        aggregator = MaxAggregation()
    elif aggregator_type == "min":
        aggregator = MinAggregation()
    elif aggregator_type == "power":
        aggregator = PowerMeanAggregation(learn=True)
    elif aggregator_type == "softmax":
        aggregator = SoftmaxAggregation(learn=True)
    elif aggregator_type == "attention":
        assert fcInputLayerSize is not None, "Input Layer size must be set for Attention Aggregator"
        aggregator = AttentionalAggregation(gate_nn=torch.nn.Linear(fcInputLayerSize-1, 1))
    elif aggregator_type == "equilibrium":
        aggregator = EquilibriumAggregation(fcInputLayerSize-1,fcInputLayerSize-1,[256,256])
    else:
        raise ValueError("Not a valid aggregator")
    
    return aggregator

class EGC(torch.nn.Module):
    def __init__(self, passes, inputLayerSize, outputLayerSize, pool, aggregators = ["symnorm"], shouldJump=True, num_heads=8, num_bases=4):
        super(EGC, self).__init__()
        self.passes = passes
        self.shouldJump = shouldJump
        self.modSize = inputLayerSize - (inputLayerSize%num_heads)

        self.egcs = nn.ModuleList([EGConv(in_channels=inputLayerSize if i == 0 else self.modSize, out_channels=self.modSize,aggregators=aggregators,num_heads=num_heads, num_bases=num_bases) for i in range(passes)])

        if self.shouldJump:
            self.jump = JumpingKnowledge('cat', (self.passes*self.modSize)+inputLayerSize)
            fcInputLayerSize = ((self.passes*self.modSize)+inputLayerSize)*len(pool)+1
        else:
            fcInputLayerSize = self.modSize*len(pool) + 1
        
        if len(pool) > 1:
            pools = []
            for pool_type in pool:
                if self.shouldJump:
                    pools+=[build_aggregators(pool_type, fcInputLayerSize=((self.passes*self.modSize)+inputLayerSize)+1)]
                else:
                    pools+=[build_aggregators(pool_type, fcInputLayerSize=(self.modSize)+1)]
            self.pool = MultiAggregation(aggrs=pools)
        else:
            self.pool = build_aggregators(pool[0], fcInputLayerSize)
    

        self.fc1 = nn.Linear(fcInputLayerSize, fcInputLayerSize//2)
        self.fc2 = nn.Linear(fcInputLayerSize//2,fcInputLayerSize//2)
        self.fcLast = nn.Linear(fcInputLayerSize//2, outputLayerSize)

    def forward(self, x, edge_index, batch, problemType=torch.FloatTensor([1])):
        if self.shouldJump:
            xs = [x]

        for egc in self.egcs: 
            out = egc(x, edge_index)
            x = f.leaky_relu(out)
            if self.shouldJump:
                xs += [x]

        if self.shouldJump:
            x = self.jump(xs)

        x = self.pool(x, batch.long())

        try:
            x = torch.cat((x, problemType.unsqueeze(1)), dim=1)
        except Exception as e:
            print(e)
            print(problemType.unsqueeze(1))
            print(x.size())
            exit()

        x = self.fc1(x)
        x = f.leaky_relu(x)
        x = self.fc2(x)
        x = f.leaky_relu(x)
        x = self.fcLast(x)

        return x