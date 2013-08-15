package ch.jachen.dev.util;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class Tree{

	private Node root;
	private int maxDepth;
	private Collection<File> excludeFiles;

    public Tree(File file, int maxDepth) {
        this(file, maxDepth, new HashSet<File>());
    }
    
    public Tree(File file, int maxDepth, Collection<File> excludeFiles) {
        this.root = new Node(file);
        this.maxDepth = maxDepth;
        this.excludeFiles = excludeFiles;
        populate();
    }
    
    public Node getRoot(){
    	return root;
    }

    public boolean isEmpty() {
        return root==null || root.getFiles().isEmpty();
    }
    
    public int getMaxDepth(){
    	return getMaxDepth(root);
    }
    
    private int getMaxDepth(Node node){
    	int level = node.getLevel();
    	for(Node n : node.getChildren()){
    		int result = getMaxDepth(n);
    		if(result>level)
    			level = result;
    	}
    	return level;
    }

    private void populate() {
    	root.children = new ArrayList<Node>();
        addTree(root, 0, maxDepth);
    }
    
    private final FileFilter filter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {				
			try {
				for(File f : excludeFiles)
					if(f.equals(pathname))
						return false;
				return !pathname.isHidden() && !pathname.getName().startsWith("@") && !pathname.getName().startsWith(".");
			} catch (Exception e) {	return false; }
		}
	};
    
    private boolean addTree(Node root, int depth, int maxDepth) {
	    List<File> children = Arrays.asList(root.data.listFiles(filter));
	    Collections.sort(children, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if(o1.isDirectory() && o2.isDirectory() ||
						o1.isFile() && o2.isFile())
					return o1.getName().compareTo(o2.getName());
				else if(!o1.isDirectory())
					return 1;
				else 
					return -1;
			}
		});
	    if (children != null && depth<=maxDepth) {
	    	boolean hasAnyFile = false;
	        for (File child : children) {
	        	Node childNode = new Node(root, child);
	        	if(child.isDirectory()){
	        		if(addTree(childNode, depth+1, maxDepth)){
	    	        	root.children.add(childNode);
	    	        	hasAnyFile=true;
	        		}
	        	}else{
		        	root.children.add(childNode);
		        	hasAnyFile=true;
	        	}
	        }
	        return hasAnyFile;
	    }
	    return false;
	}
    
    public int countFiles(){
    	return root.getFiles().toArray().length;
    }

    public void print() {
        print(this.root);
    }

    private void print(Node n) {
        if(n==null)
            return;
        for(Node c : n.children) {
            System.out.println(c.data + " ");
            print(c);
        }
    }
    
    public void printTree() {
        StringBuilder sb = new StringBuilder();
        printDirectoryTree(this.root, -1, sb);
        System.out.println(sb.toString());    	
    }

    public void printTree(int maxParentsLevel) {
        StringBuilder sb = new StringBuilder();
        Node node = filesAndParents(this.root, maxParentsLevel);
        printDirectoryTree(node, -1, sb);
        System.out.println(sb.toString());    	
    }
    
    private void printDirectoryTree(Node n, int level, StringBuilder sb) {
        if(n==null)
            return;
        if(!n.isRoot()){
	        sb.append(getIndentString(level));
	        if(level>0)
	        	sb.append("+--");
	        else 
	        	sb.append("  ");
	        sb.append(n.getFile().getName());
	        sb.append("/");
	        sb.append("\n");
        }
        for(Node c : n.children) {
        	if(c.getFile().isDirectory())
        		printDirectoryTree(c, level+1, sb);
        	else printNodeFile(c, level+1, sb);
        }
    }
    
    private Node filesAndParents(Node root, int maxParentsLevel){
    	Node result = new Node(root.getFile());
    	
    	for(Node n : root.getFiles()){
    		Node current = n;
    		for(int i=0; i < maxParentsLevel; i++){
    			if(current.getParent()!=null && !current.getParent().isRoot())
    				current = current.getParent();
    			else
    				break;
    		}
    		if(!result.getChildren().contains(current))
    			result.addChild(current);
    	}
    	
    	return result;
    }
    
    private static void printNodeFile(Node node, int indent, StringBuilder sb) {
    	if(indent>0){
	        sb.append(getIndentString(indent));
	        sb.append("+--");
    	} else
    		sb.append("  ");
        sb.append(node.getFile().getName());
        sb.append("\n");
    }
    
    private static String getIndentString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
        	if(i==0)
        		sb.append("  ");
            sb.append("|  ");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        Tree t = new Tree(new File(args[0]), 3);
        t.print();
        t.printTree();
        t.printTree(1);
        System.out.println("Nb files: " + t.countFiles());
    }
}