package com.vns.pdf.impl;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;

public class PdfBookmarks {
    public static void main(String[] args) throws IOException {
        args = new String[]{"docs/jls8.pdf"};
        if (args.length != 1) {
            usage();
        } else {
            PDDocument document = null;
            try {
                document = PDDocument.load(new File(args[0]));
                PdfBookmarks meta = new PdfBookmarks();
                PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
                if (outline != null) {
                    meta.printBookmark(outline, "");
                } else {
                    System.out.println("This document does not contain any bookmarks");
                }
            } finally {
                if (document != null) {
                    document.close();
                }
            }
        }
    }
    
    private static void usage() {
        System.err.println("Usage: java " + PdfBookmarks.class.getName() + " <input-pdf>");
    }
    
    public void printBookmark(PDOutlineNode bookmark, String indentation) throws IOException {
        PDOutlineItem current = bookmark.getFirstChild();
        while (current != null) {
            System.out.println(indentation + current.getTitle());
            printBookmark(current, indentation + "    ");
            current = current.getNextSibling();
        }
        
    }
}