package cn.edu.nju.software.frontend.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph<T>{
    private final Map<T, Set<T>> nodes;

    public Graph() {
        nodes = new HashMap<>();
    }

    public void addNode(T node) {
        if(!nodes.containsKey(node)){
            nodes.put(node, new HashSet<>());
        }
    }

    public void addEdge(T from, T to) {
        if(!nodes.containsKey(from)) {
            nodes.put(from, new HashSet<>());
        }
        Set<T> set=nodes.get(from);
        set.add(to);
    }

    public void removeEdge(T from, T to) {
        if (nodes.containsKey(from)) {
            nodes.get(from).remove(to);
        }
    }

    public Set<T> getNeighbors(T node) {
        return nodes.getOrDefault(node, new HashSet<>());
    }

    public Set<T> getAllNodes() {
        return nodes.keySet();
    }

    public Set<Edge<T>> getAllEdges() {
        Set<Edge<T>> edges = new HashSet<>();
        for(T from : nodes.keySet()) {
            for(T to : nodes.get(from)) {
                edges.add(new Edge<>(from,to));
            }
        }
        return edges;
    }

    public boolean isEmpty(){
        return nodes.isEmpty();
    }
}

