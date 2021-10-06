#!/bin/bash

if command -v conda &> /dev/null
then
    CONDA=1
elif command -v pip3 &> /dev/null
then
    PIP=1
else
    echo "Neither conda or pip3 are installed. Please install one and re-attempt the install"
fi

if command -v ant &> /dev/null
then
    echo "ant is not installed. Please install it and re-attempt the install "
fi

if ! python3 -c "import torch" &> /dev/null
then
    echo "---------------------------------------------"
    echo "Installing PyTorch now:"
    echo "---------------------------------------------"
    if CONDA
    then
        conda install pytorch torchvision torchaudio cpuonly -c pytorch
    else
        pip3 install torch==1.9.1+cpu torchvision==0.10.1+cpu torchaudio==0.9.1 -f https://download.pytorch.org/whl/torch_stable.html
    fi
else
    echo "---------------------------------------------"
    echo "               PyTorch detected              "
    echo "---------------------------------------------"
fi
if ! python3 -c "import torch_geometric" &> /dev/null
then
    echo "---------------------------------------------"
    echo "Installing PyTorch-Geometric now:"
    echo "---------------------------------------------"
    if CONDA
    then
        conda install pyg -c pyg -c conda-forge
    else
        pip install torch-scatter torch-sparse torch-cluster torch-spline-conv torch-geometric -f https://data.pyg.org/whl/torch-1.9.0+cpu.html
    fi
else
    echo "---------------------------------------------"
    echo "         PyTorch-Geometric detected          "
    echo "---------------------------------------------"
fi

if command -v graph-builder &> /dev/null
then
    echo "---------------------------------------------"
    echo " graph-builder already in path. Either setup "
    echo " script has been executed or this is another "
    echo " graph-builder which conflicts with ours     "
    echo "---------------------------------------------"
else
    echo
    echo "---------------------------------------------"
    echo "           Building graph-builder            "
    echo "---------------------------------------------"
    echo
    mkdir llvm-project/build
    pushd llvm-project/build
    if command -v ninja &> /dev/null
    then
        cmake -DCMAKE_BUILD_TYPE=Release -DLLVM_ENABLE_PROJECTS="clang;clang-tools-extra" -G Ninja ..
        ninja graph-builder
    else
        cmake -DCMAKE_BUILD_TYPE=Release -DLLVM_ENABLE_PROJECTS="clang;clang-tools-extra" ..
        make graph-builder
    fi

    if [ ! -d "${HOME}/.local/bin" ]
    then   
        mkdir ${HOME}/.local/bin
    fi

    ln ${PWD}/llvm-project/build/bin/graph-builder -s ${HOME}/.local/bin

    echo
    echo "---------------------------------------------"
    echo " graph-builder added to ${HOME}/.local/bin   "
    echo " Make sure ${HOME}/.local/bin is in path     "
    echo " E.G. add the following command to ~/.bashrc "
    echo " export PATH=$PATH:$HOME/.local/bin          "
    echo "---------------------------------------------"
    echo
fi

echo
echo "---------------------------------------------"
echo "             Building CPAChecker             "
echo "---------------------------------------------"
echo

