// (c) 2021 Michael Schierl
// Licensed under MIT License
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class RemoveLeaves2GraphML {
	public static void main(String[] args) throws Exception {
		String[] lines = Files.readAllLines(new File(args[0]).toPath()).toArray(new String[0]);
		Map<String, Integer> lineIndex = new HashMap<>();
		for (int i = 0; i < lines.length; i++) {
			String pkg = lines[i].split(" -> ")[0];
			lineIndex.put(pkg, i);
		}
		boolean[][] deps = new boolean[lines.length][lines.length];
		int[] outgoingEdges = new int[lines.length], incomingEdges = new int[lines.length];
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].endsWith("->"))
				continue;
			String[] pkgs = lines[i].split(" -> ")[1].split(" ");
			for (String pkg : pkgs) {
				Integer pidx = lineIndex.get(pkg);
				if (pidx != null) {
					deps[i][pidx] = true;
					outgoingEdges[i]++;
					incomingEdges[pidx]++;
				}
			}
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = 0; i < lines.length; i++) {
				if (outgoingEdges[i] == 0 && incomingEdges[i] != 0) {
					changed = true;
					for (int j = 0; j < lines.length; j++) {
						if (deps[j][i]) {
							deps[j][i] = false;
							outgoingEdges[j]--;
							incomingEdges[i]--;
						}
					}
				}
				if (outgoingEdges[i] != 0 && incomingEdges[i] == 0) {
					changed = true;
					for (int j = 0; j < lines.length; j++) {
						if (deps[i][j]) {
							deps[i][j] = false;
							outgoingEdges[i]--;
							incomingEdges[j]--;
						}
					}
				}
			}
		}
		if (false) {
			for (int i = 0; i < lines.length; i++) {
				if (incomingEdges[i] == 0 && outgoingEdges[i] == 0)
					continue;
				System.out.print(lines[i].split(" -> ")[0] + " ->");
				for (int j = 0; j < lines.length; j++) {
					if (deps[i][j]) {
						System.out.print(" " + lines[j].split(" -> ")[0]);
					}
				}
				System.out.println();
			}
		} else {
			System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			System.out.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"  ");
			System.out.println("      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			System.out.println("      xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns ");
			System.out.println("        http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">");
			System.out.println("  <key id=\"name\" for=\"node\" attr.name=\"name\" attr.type=\"string\" />");
			System.out.println("  <key id=\"version\" for=\"node\" attr.name=\"version\" attr.type=\"string\"><default>?</default></key>");
			System.out.println("  <graph id=\"G\" edgedefault=\"directed\">");
			for (int i = 0; i < lines.length; i++) {
				if (incomingEdges[i] == 0 && outgoingEdges[i] == 0)
					continue;
				System.out.println("<node id=\"n"+i+"\"><data key=\"name\">" + lines[i].split(" -> ")[0] + "</data></node>");
			}
			int ecnt = 0;
			for (int i = 0; i < lines.length; i++) {
				if (incomingEdges[i] == 0 && outgoingEdges[i] == 0)
					continue;
				for (int j = 0; j < lines.length; j++) {
					if (deps[i][j]) {
						System.out.println("    <edge id=\"e" + ecnt + "\" source=\"n" + i + "\" target=\"n" + j + "\"/>");
						ecnt++;
					}
				}
			}
			System.out.println("  </graph>");
			System.out.println("</graphml>");
		}
	}
}
