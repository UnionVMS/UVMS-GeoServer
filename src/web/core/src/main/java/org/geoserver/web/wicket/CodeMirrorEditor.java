/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.request.WebClientInfo;

/**
 * A XML editor based on CodeMirror
 * @author Andrea Aime 
 */
@SuppressWarnings("serial")
public class CodeMirrorEditor extends FormComponentPanel<String> {

    public static final ResourceReference REFERENCE = new ResourceReference(
            CodeMirrorEditor.class, "js/codemirror/js/codemirror.js");
    
    public static final ResourceReference CSS_REFERENCE = new ResourceReference(
            CodeMirrorEditor.class, "js/codemirror/css/codemirror.css");
    
    public static final ResourceReference[] MODES = new ResourceReference[] {
        new ResourceReference(CodeMirrorEditor.class, "js/codemirror/js/xml.js"),
        new ResourceReference(CodeMirrorEditor.class, "js/codemirror/js/clike.js"),
        new ResourceReference(CodeMirrorEditor.class, "js/codemirror/js/groovy.js"),
        new ResourceReference(CodeMirrorEditor.class, "js/codemirror/js/javascript.js"),
        new ResourceReference(CodeMirrorEditor.class, "js/codemirror/js/python.js"),
        new ResourceReference(CodeMirrorEditor.class, "js/codemirror/js/ruby.js"),
        new ResourceReference(CodeMirrorEditor.class, "js/codemirror/js/css.js")
    };
    

    private TextArea<String> editor;

    private WebMarkupContainer container;

    private String mode;
    
    public CodeMirrorEditor(String id, String mode, IModel<String> model) {
        super(id, model);
        this.mode = mode;
        
        // figure out if we're running against a browser supported by CodeMirror
        boolean enableCodeMirror = isCodeMirrorSupported();

        container = new WebMarkupContainer("editorContainer");
        container.setOutputMarkupId(true);
        add(container);
        
        WebMarkupContainer toolbar = new WebMarkupContainer("toolbar");
        toolbar.setVisible(enableCodeMirror);
        container.add(toolbar);

        WebMarkupContainer editorParent = new WebMarkupContainer("editorParent");
        if (enableCodeMirror) {
            editorParent.add(new SimpleAttributeModifier("style", "border: 1px solid black;"));
        }
        container.add(editorParent);
        editor = new TextArea<String>("editor", model);
        editorParent.add(editor);
        editor.setOutputMarkupId(true);

        if (enableCodeMirror) {
            editor.add(new CodeMirrorBehavior());
        } else {
            editor.add(new SimpleAttributeModifier("style", "width:100%"));
        }
    }

    private boolean isCodeMirrorSupported() {
        boolean enableCodeMirror = true;
        WebClientInfo clientInfo = (WebClientInfo) WebRequestCycle.get().getClientInfo();
        ClientProperties clientProperties = clientInfo.getProperties();
        if (clientProperties.isBrowserInternetExplorer()) {
            ClientProperties props = extractIEVersion(clientProperties.getNavigatorUserAgent());
            enableCodeMirror = clientProperties.getBrowserVersionMajor() >= 8
                || props.getBrowserVersionMajor() >= 8;
        } else if (clientProperties.isBrowserMozillaFirefox()) {
            ClientProperties props = extractFirefoxVersion(clientProperties.getNavigatorUserAgent());
            enableCodeMirror = clientProperties.getBrowserVersionMajor() >= 3
                || props.getBrowserVersionMajor() >= 3;
        } else if (clientProperties.isBrowserSafari()) {
            ClientProperties props = extractSafariVersion(clientProperties.getNavigatorAppVersion());
            enableCodeMirror = clientProperties.getBrowserVersionMajor() > 5
                    || (clientProperties.getBrowserVersionMajor() == 5
                    && clientProperties.getBrowserVersionMinor() >= 2)
                    || props.getBrowserVersionMajor() > 5
                    || (props.getBrowserVersionMajor() == 5
                    && props.getBrowserVersionMinor() >= 2);
        } else if (clientProperties.isBrowserOpera()) {
            ClientProperties props = extractOperaVersion(clientProperties.getNavigatorAppVersion());
            enableCodeMirror = clientProperties.getBrowserVersionMajor() >= 9
                || props.getBrowserVersionMajor() >= 9;
        }
        return enableCodeMirror;
    }

    private ClientProperties extractIEVersion(String userAgent) {
        ClientProperties props = new ClientProperties();
        props.setBrowserVersionMajor(-1);
        props.setBrowserVersionMinor(-1);
        if (userAgent != null ) {
            String userAgencyLc = userAgent.toLowerCase();
            String pattern;
            if (userAgencyLc.contains("like gecko")) {
                pattern = "rv:(\\d+)\\.(\\d+)";
            } else {
                pattern = "msie (\\d+)\\.(\\d+)";
            }
            setMajorMinorVersionByPattern(userAgencyLc, pattern, props);
        }
        return props;
    }


    private ClientProperties extractFirefoxVersion(String userAgent) {
        ClientProperties props = new ClientProperties();
        props.setBrowserVersionMajor(-1);
        props.setBrowserVersionMinor(-1);
        if (userAgent != null) {
            String userAgencyLc = userAgent.toLowerCase();
            props.setBrowserVersionMajor(-1);
            props.setBrowserVersionMinor(-1);
            setMajorMinorVersionByPattern(userAgencyLc, "firefox/(\\d+)\\.(\\d+)", props);
        }
        return props;
    }

    private ClientProperties extractOperaVersion(String userAgent) {
        ClientProperties props = new ClientProperties();
        props.setBrowserVersionMajor(-1);
        props.setBrowserVersionMinor(-1);
        if (userAgent != null) {
            String userAgencyLc = userAgent.toLowerCase();
            if (userAgencyLc.startsWith("opera/") && userAgencyLc.contains("version/")) {
                setMajorMinorVersionByPattern(userAgencyLc, "version/(\\d+)\\.(\\d+)", props);
            } else if (userAgencyLc.startsWith("opera/") && !userAgencyLc.contains("version/")) {
                setMajorMinorVersionByPattern(userAgencyLc, "opera/(\\d+)\\.(\\d+)", props);
            } else {
                setMajorMinorVersionByPattern(userAgencyLc, "opera (\\d+)\\.(\\d+)", props);
            }
        }
        return props;
    }

    private ClientProperties extractSafariVersion(String userAgent) {
        ClientProperties props = new ClientProperties();
        props.setBrowserVersionMajor(-1);
        props.setBrowserVersionMinor(-1);
        if (userAgent != null) {
            String userAgencyLc = userAgent.toLowerCase();
            setMajorMinorVersionByPattern(userAgencyLc, "version/(\\d+)\\.(\\d+)", props);
        }
        return props;
    }

    private ClientProperties extractChromeVersion(String userAgent) {
        ClientProperties props = new ClientProperties();
        props.setBrowserVersionMajor(-1);
        props.setBrowserVersionMinor(-1);
        if (userAgent != null) {
            String userAgencyLc = userAgent.toLowerCase();
            setMajorMinorVersionByPattern(userAgencyLc, "chrome/(\\d+)\\.(\\d+)", props);
        }
        return props;
    }

    private void setMajorMinorVersionByPattern(String userAgent, String patternString, ClientProperties properties) {
        Matcher matcher = Pattern.compile(patternString).matcher(userAgent);
        if (matcher.find()) {
            properties.setBrowserVersionMajor(Integer.parseInt(matcher.group(1)));
            properties.setBrowserVersionMinor(Integer.parseInt(matcher.group(2)));
        }
    }

    public CodeMirrorEditor(String id, IModel<String> model) {
        this(id, "xml", model);
    }
    
    @Override
    protected void convertInput() {
        editor.processInput();
        setConvertedInput(editor.getConvertedInput());
    }
    
    @Override
    public String getInput() {
        return editor.getInput();
    }
    
    public void setTextAreaMarkupId(String id) {
        editor.setMarkupId(id);
    }
    
    public String getTextAreaMarkupId() {
        return editor.getMarkupId();
    }
    
    public void setMode(String mode) {
        this.mode = mode;
        if (AjaxRequestTarget.get() != null) {
            String javascript = "document.gsEditors." + editor.getMarkupId() + ".setOption('mode', '" + mode + "');";
            AjaxRequestTarget.get().appendJavascript(javascript);
        }
    }
    
    public void reset() {
        super.validate();
        editor.validate();
        editor.clearInput();
    }
    
    public IAjaxCallDecorator getSaveDecorator() {
        // we need to force CodeMirror to update the textarea contents (which it hid)
        // before submitting the form, otherwise the validation will use the old contents
        return new AjaxCallDecorator() {
            @Override
            public CharSequence decorateScript(CharSequence script) {
                // textarea.value = codemirrorinstance.getCode()
                String id = getTextAreaMarkupId();
                return "if (document.gsEditors) { document.getElementById('" + id + "').value = document.gsEditors." + id + ".getValue(); }" + script;
            }
        };
    }
    
    class CodeMirrorBehavior extends AbstractBehavior {

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            // Add CSS
            response.renderCSSReference(CSS_REFERENCE);
            // Add JS
            response.renderJavascriptReference(REFERENCE);
            // Add Modes
            for(ResourceReference mode : MODES) {
                response.renderJavascriptReference(mode);
            }
            
            response.renderOnDomReadyJavascript(getInitJavascript());
        }

        private String getInitJavascript() {
            InputStream is = CodeMirrorEditor.class.getResourceAsStream("CodeMirrorEditor.js");
            String js = convertStreamToString(is);
            js = js.replaceAll("\\$componentId", editor.getMarkupId());
            js = js.replaceAll("\\$mode", mode);
            js = js.replaceAll("\\$container", container.getMarkupId());
            return js;
        }

        public String convertStreamToString(InputStream is) {
            /*
             * To convert the InputStream to String we use the Reader.read(char[] buffer) method. We
             * iterate until the Reader return -1 which means there's no more data to read. We use
             * the StringWriter class to produce the string.
             */
            try {
                if (is != null) {
                    Writer writer = new StringWriter();

                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        is.close();
                    }
                    return writer.toString();
                } else {
                    return "";
                }
            } catch (IOException e) {
                throw new RuntimeException("Did not expect this one...", e);
            }
        }

    }

}
