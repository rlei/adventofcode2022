import std.stdio, std.array, std.algorithm, std.conv, std.range, std.sumtype, std.string;

// A generic tree node type that's not specific to this problem
class TreeNode(Data, alias IdMapper) {
    alias Node = TreeNode!(Data, IdMapper);

    Data data;
    Node parent;

    Node[string] childNodes;
    Data[string] leaves;

    this(Data data) {
        this.data = data;
    } 

    void addChildNode(Node node) {
        node.parent = this;
        this.childNodes[IdMapper(node.data)] = node;
    }

    // Leaf has only data, no children
    void addLeaf(Data leaf) {
        this.leaves[IdMapper(leaf)] = leaf;
    }

    Node getChildNode(string name) {
        return childNodes[name];
    }

    Node getParent() {
        return parent;
    }

    Data getData() {
        return data;
    }

    void setData(Data data) {
        this.data = data;
    }

    Node[] getChildNodes() {
        return childNodes.values;
    }

    Data[] getLeaves() {
        return leaves.values;
    }

    // note this only traverse non leaf nodes
    void dfsPostOrderTraverse(void delegate(Node) visitor) {
        foreach (node; this.childNodes) {
            node.dfsPostOrderTraverse(visitor);
        }
        visitor(this);
    }
}

struct FileInfo {
    string name;
    int size;
}

alias DirNode = TreeNode!(FileInfo, (FileInfo f) { return f.name; }).Node;

DirNode buildTree() {
    DirNode root, current;
    foreach (line; stdin.byLine) {
        auto words = split(line);
        if (words[0] == "$") {
            switch (words[1]) {
                case "cd":
                    auto name = words[2].idup();
                    if (name == "..") {
                        current = current.getParent();
                    } else {
                        if (current is null) {
                            // root
                            current = new DirNode(FileInfo(name, 0));
                            root = current;
                        } else {
                            // assuming all children are already made known by "ls"
                            current = current.getChildNode(name);
                        }
                    }
                    // writeln("current folder ", current.getData().name);
                    break;

                case "ls":
                    // do nothing
                    break;

                default:
                    throw new Exception("unknown command");
            }
        } else {
            // For now it must be output of last "ls"
            auto name = words[1].idup();
            if (words[0] == "dir") {
                current.addChildNode(new DirNode(FileInfo(name, 0)));
            } else {
                auto size = to!int(words[0]);
                // writeln("file ", name, " ", size);
                current.addLeaf(FileInfo(name, size));
            }
        }
    }
    return root;
}

void main() {
    auto root = buildTree();
    int sumOfDirsOfSizeAtMost100000 = 0;
    int[] dirSizes;

    root.dfsPostOrderTraverse((DirNode dir) {
        auto size = chain(dir.getLeaves(), dir.getChildNodes().map!(node => node.getData()))
            .map!(data => data.size)
            .sum();
        auto dirInfo = dir.getData();
        /*
        writeln("dir >> ", dirInfo.name, "; size ", size);
        foreach (file; dir.getLeaves()) {
            writeln("file ", file.name, "; size ", file.size);
        }
        writeln("dir <<");
        */
        dir.setData(FileInfo(dirInfo.name, size));

        dirSizes ~= size;
        if (size <= 100000) {
            sumOfDirsOfSizeAtMost100000 += size;
        }
    });
    writeln("Sum of sizes of folders smaller than 100000: ", sumOfDirsOfSizeAtMost100000);

    auto spaceToFree = 30000000 - (70000000 - root.getData().size);
    writeln("Size of the folder to delete: ", dirSizes.filter!(n => n >= spaceToFree).minElement());
}
