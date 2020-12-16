from lib.utils import evaluate
from lib.ggnn import GGNN
from numpy.random import shuffle
from numpy import load as npload
import torch
from sys import argv

possible = ["PA", "KI", "VA-NoCegar", "BMC", "VA-Cegar", "Unknown", ]

nodes = npload(argv[1]+"/tempFile.json.npz")['node_rep']
nodes = torch.from_numpy(nodes)

model = GGNN(passes=0, numEdgeSets=0)
model.load_state_dict(torch.load(argv[2]))

prediction = (-model(nodes, [], torch.FloatTensor([1]))).argsort()

order = [possible[x] for x in prediction]

outfile = open("ggnnLogFiles/prediction.txt", 'w')

for analysis in order:
    outfile.write(analysis+"\n")