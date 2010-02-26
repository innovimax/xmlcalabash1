package com.xmlcalabash.util;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;

import javax.xml.transform.TransformerException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.ErrorListener;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 9, 2009
 * Time: 7:13:02 AM
 *
 * This listener collects messages to send to the error port if applicable.
 */
public class StepErrorListener implements ErrorListener {
    private static QName c_error = new QName(XProcConstants.NS_XPROC_STEP, "error");
    private static QName _name = new QName("", "name");
    private static QName _type = new QName("", "type");
    private static QName _href = new QName("", "href");
    private static QName _line = new QName("", "line");
    private static QName _column = new QName("", "column");
    private static QName _code = new QName("", "code");

    private XProcRuntime runtime = null;
    private URI baseURI = null;

    public StepErrorListener(XProcRuntime runtime) {
        super();
        this.runtime = runtime;
        baseURI = runtime.getStaticBaseURI();
    }

    public void error(TransformerException exception) throws TransformerException {
        if (!report("error", exception)) {
            runtime.error(exception);
        }
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        if (!report("fatal-error", exception)) {
            runtime.error(exception);
        }
    }

    public void warning(TransformerException exception) throws TransformerException {
        if (!report("warning", exception)) {
            // XProc doesn't have recoverable exceptions...
            runtime.error(exception);
        }
    }

    private boolean report(String type, TransformerException exception) {
        TreeWriter writer = new TreeWriter(runtime);

        writer.startDocument(baseURI);
        writer.addStartElement(c_error);
        writer.addAttribute(_type, type);

        StructuredQName qCode = null;
        if (exception instanceof XPathException) {
            qCode = ((XPathException) exception).getErrorCodeQName();
        }
        if (qCode == null && exception.getException() instanceof XPathException) {
            qCode = ((XPathException) exception.getException()).getErrorCodeQName();
        }
        if (qCode != null) {
            writer.addAttribute(_code, qCode.getDisplayName());
        }

        if (exception.getLocator() != null) {
            SourceLocator loc = exception.getLocator();
            boolean done = false;
            while (!done && loc == null) {
                if (exception.getException() instanceof TransformerException) {
                    exception = (TransformerException) exception.getException();
                    loc = exception.getLocator();
                } else if (exception.getCause() instanceof TransformerException) {
                    exception = (TransformerException) exception.getCause();
                    loc = exception.getLocator();
                } else {
                    done = true;
                }
            }

            if (loc != null) {
                if (loc.getSystemId() != null && !"".equals(loc.getSystemId())) {
                    writer.addAttribute(_href, loc.getSystemId());
                }

                if (loc.getLineNumber() != -1) {
                    writer.addAttribute(_line, ""+loc.getLineNumber());
                }

                if (loc.getColumnNumber() != -1) {
                    writer.addAttribute(_column, ""+loc.getColumnNumber());
                }
            }
        }


        writer.startContent();
        writer.addText(exception.toString());
        writer.addEndElement();
        writer.endDocument();

        XdmNode node = writer.getResult();

        return runtime.getXProcData().catchError(node);
    }
}