package ch.jachen.dev.util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Node {
    File data;
    Node parent;
    List<Node> children;
    
    public Node(File data) {
    	this(null, data);      
    }

    public Node(Node parent, File data) {
    	this.parent = parent;
        this.data = data;
        children = new ArrayList<Node>();            
    }

    public void addChild(Node node){
    	this.children.add(node);
    }
    
    public Node getChild(File data) {
        for(Node n : children)
        	if(n.data.equals(data))
                return n;
        return null;
    }
    
    public File getFile(){
    	return data;
    }
    
    public Node getParent(){
    	return parent;
    }
    
    public List<Node> getChildren(){
    	return children;
    }
    
    public Set<Node> getFiles(){  		
    	return getFiles(this);
    }
    
    private Set<Node> getFiles(Node node){
    	Set<Node> nodes = new LinkedHashSet<Node>();
    	if(!node.isDirectory())
    		nodes.add(node);
    	for(Node n : node.getChildren())
    		nodes.addAll(getFiles(n));
    	return nodes;
    }
    
    public List<Node> getSiblings(){
    	List<Node> siblings = new ArrayList<Node>(parent.getChildren());
    	if(!siblings.remove(this))
    		throw new IllegalStateException("Illegal state: "+this+" not presents in this.getParent().getChildren() !");
    	return siblings;
    }
    
    public boolean isRoot(){
    	return parent == null;
    }
    
    public boolean isStandaloneFile(){
    	return !isDirectory() && (isRoot() || parent.isRoot());
    }
    
    public boolean isStandaloneSet(){
    	return isDirectory() && (isRoot() || parent.isRoot());
    }
    
    public boolean isDirectory(){
    	return data.isDirectory();
    }
    
    public boolean hasChildren(){
    	return !children.isEmpty();
    }
    
    public boolean hasChildrenFiles(){
    	for(Node n : children)
    		if(!n.isDirectory())
    			return true;
    	return false;
    }
    
    public List<Node> getParents(){
    	List<Node> parents = new ArrayList<Node>();
    	Node current = this.getParent();
    	while(current!=null){
    		parents.add(current);
    		current = current.getParent();
    	}
    	return parents;
    }
    
    public int getLevel(){
    	int level = 0;
    	Node current = this.getParent();
    	while(current!=null){
    		current = current.getParent();
    		level++;
    	}
    	return level;
    }
}