from lib.gnn import GAT
from numpy import load as npload
import torch
from torch_geometric.data import Data, Batch
from sys import argv

possible = ["PA", "KI", "VA-NoCegar", "BMC", "VA-Cegar", "Unknown", ]

nodes = npload(argv[1])['node_rep']
nodes = torch.from_numpy(nodes)

edges = npload(argv[2])

edges_tensor = [torch.from_numpy(edges[edgeSet]) for edgeSet in edges]

edge_labels = torch.cat([torch.full((len(edges_tensor[i]),1),i) for i in range(len(edges_tensor))], dim=0)        
edges_tensor = torch.cat(edges_tensor).transpose(0,1).long()

data = Data(x=nodes.float(), edge_index=edges_tensor, edge_attr=edge_labels, problemType=torch.FloatTensor([1]))

data = Batch.from_data_list([data])

model = GAT(passes=1, numEdgeSets=3, inputLayerSize=nodes.size(1), outputLayerSize=len(possible), numAttentionLayers=5, mode='cat', pool='mean', k='3')
model.load_state_dict(torch.load(argv[3]))

prediction = (-model(x=data.x, edge_index=data.edge_index, edge_attr=data.edge_attr, problemType=data.problemType, batch=data.batch)).argsort()

order = [possible[x] for x in prediction.squeeze()]

outfile = open("ggnnLogFiles/prediction.txt", 'w')

for analysis in order:
    outfile.write(analysis+"\n")