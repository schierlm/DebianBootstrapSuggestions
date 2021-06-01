// (c) 2021 Michael Schierl
// Licensed under MIT License
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class RemoveLeavesFromGraph {

	private static final int MAX_ID_COUNT = 65536, MAX_EDGE_COUNT = 4_000_000;

	static String[] names = new String[MAX_ID_COUNT];
	static int[] edgeFrom = new int[MAX_EDGE_COUNT], edgeTo = new int[MAX_EDGE_COUNT];
	static int edgeCount=0;

	public static void main(String[] args) throws Exception {

		try (InputStream in = new GZIPInputStream(new FileInputStream(args[0]))) {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(new DefaultHandler() {

				int currentID = -1;
				boolean inName = false;

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					if (Arrays.asList("graphml", "key", "default", "graph").contains(localName))
						return;
					else if (localName.equals("node")) {
						currentID = Integer.parseInt(attributes.getValue("id").substring(1));
					} else if (localName.equals("data")) {
						if (attributes.getValue("key").equals("name")) {
							inName = true;
						}
					} else if (localName.equals("edge")) {
						int source = Integer.parseInt(attributes.getValue("source").substring(1));
						int target = Integer.parseInt(attributes.getValue("target").substring(1));
						edgeFrom[edgeCount] = source;
						edgeTo[edgeCount] = target;
						edgeCount++;
					} else {
						throw new RuntimeException();
					}
				}

				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException {
					if (localName.equals("node"))
						currentID = -1;
					if (localName.equals("data") && inName) {
						inName = false;
					}
				}

				@Override
				public void characters(char[] ch, int start, int length) throws SAXException {
					if (inName) {
						names[currentID] = new String(ch, start, length);
					}
				}
			});
			xmlReader.parse(new InputSource(in));
			int[] incount = new int[MAX_ID_COUNT], outcount = new int[MAX_ID_COUNT];
			boolean changed = true;
			while (changed) {
				changed = false;
				for(int i= 0; i< MAX_ID_COUNT; i++) {
					if (incount[i] != -1) incount[i] = 0;
					if (outcount[i] != -1) outcount[i] = 0;
				}
				for(int i=0; i<edgeCount; i++) {
					if (outcount[edgeFrom[i]] != -1 && incount[edgeTo[i]] != -1) {
						outcount[edgeFrom[i]]++;
						incount[edgeTo[i]]++;
					}
				}
				for(int i= 0; i< MAX_ID_COUNT; i++) {
					if (incount[i] == 0 || outcount[i] == 0) {
						incount[i] = -1; outcount[i] = -1;
						changed = true;
						if (names[i] != null) {
							System.out.println(names[i]);
						}
					}
				}
			}
			System.out.println("=======");
			for(int i= 0; i< MAX_ID_COUNT; i++) {
				if (incount[i] != -1 && names[i] != null) {
						System.out.println(names[i]);
				}
			}
		}
	}
}
