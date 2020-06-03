package model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class Tree {
    private Tree left;
    private Tree right;
    private double key;


    public Tree(double key) {
        this.key = key;
    }

    public void insert(Tree tree) {
        if (tree.key < key)
            if (left != null) {
                left.insert(tree);
            }
            else {
                left = tree;
            }
        else {
            if (right != null) {
                right.insert(tree);
            }
            else {
                right = tree;
            }
        }
    }

    public ObservableList<Double> toList() {
        ObservableList<Double> treeAsList = FXCollections.observableArrayList();
        traverseAndAdd(treeAsList);
        return treeAsList;
    }

    private void traverseAndAdd(ObservableList<Double> treeAsList) {
        if (left != null) {
            left.toList();
        }

        treeAsList.add(this.key);

        if (right != null) {
            right.toList();
        }
    }
}
