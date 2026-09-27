package in.sahitya.reader;

import android.os.Build;
import android.text.Html;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public final class DocumentText {
    private DocumentText() {}
    public static String read(String path, String format) throws Exception {
        if ("TXT".equals(format)) { try (InputStream in = new FileInputStream(path)) { return new String(readAll(in), StandardCharsets.UTF_8); } }
        if ("EPUB".equals(format)) return epub(path);
        return "";
    }
    private static String epub(String path) throws Exception {
        try (ZipFile zip = new ZipFile(path)) {
            String opf = null;
            try (InputStream in = zip.getInputStream(zip.getEntry("META-INF/container.xml"))) {
                Document d = factory().newDocumentBuilder().parse(in);
                NodeList n = d.getElementsByTagName("rootfile"); if (n.getLength() > 0) opf = ((Element)n.item(0)).getAttribute("full-path");
            }
            if (opf == null) throw new IOException("This EPUB has no readable package file.");
            String base = opf.contains("/") ? opf.substring(0, opf.lastIndexOf('/') + 1) : "";
            Document pkg;
            try (InputStream in = zip.getInputStream(zip.getEntry(opf))) { pkg = factory().newDocumentBuilder().parse(in); }
            java.util.Map<String,String> files = new java.util.HashMap<>(); NodeList items = pkg.getElementsByTagName("item");
            for (int i=0;i<items.getLength();i++) { Element e=(Element)items.item(i); files.put(e.getAttribute("id"), base + e.getAttribute("href")); }
            NodeList spine = pkg.getElementsByTagName("itemref"); StringBuilder out = new StringBuilder();
            for (int i=0;i<spine.getLength();i++) {
                String name = files.get(((Element)spine.item(i)).getAttribute("idref")); if (name == null) continue;
                ZipEntry entry = zip.getEntry(name); if (entry == null) continue;
                try (InputStream in = zip.getInputStream(entry)) { String html=new String(readAll(in), StandardCharsets.UTF_8); out.append(Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(html)).append("\n\n"); }
            }
            return out.toString().trim();
        }
    }
    private static DocumentBuilderFactory factory() throws Exception { DocumentBuilderFactory f=DocumentBuilderFactory.newInstance(); f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true); f.setFeature("http://xml.org/sax/features/external-general-entities",false); f.setFeature("http://xml.org/sax/features/external-parameter-entities",false); f.setExpandEntityReferences(false); return f; }
    private static byte[] readAll(InputStream in) throws IOException { try (ByteArrayOutputStream out = new ByteArrayOutputStream()) { byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1)out.write(b,0,n); return out.toByteArray(); } }
}
