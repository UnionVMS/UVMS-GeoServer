/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.awt.image.SampleModel;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.jai.ImageLayout;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.coverage.grid.io.GridCoverage2DReader;

/**
 * Base page for {@link CoverageView} creation/editing
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@SuppressWarnings("serial")
public abstract class CoverageViewAbstractPage extends GeoServerSecuredPage {

    public static final String COVERAGESTORE = "storeName";

    public static final String WORKSPACE = "wsName";
    
    static final String COVERAGE_VIEW_NAME = "COVERAGEVIEW_NAME";

    String storeId;

    String coverageInfoId;

    String definition;

    String name;

    boolean newCoverage;

    CoverageInfo coverageViewInfo;

    List<String> availableCoverages;

    List<String> selectedCoverages;

    List<CoverageBand> outputBands;

    CoverageViewEditor coverageEditor;

    public CoverageViewAbstractPage(PageParameters params) throws IOException {
        this(params.getString(WORKSPACE), params.getString(COVERAGESTORE), null, null);
    }

    @SuppressWarnings("deprecation")
    public CoverageViewAbstractPage(String workspaceName, String storeName, String coverageName,
            CoverageInfo coverageInfo) throws IOException {
        storeId = getCatalog().getStoreByName(workspaceName, storeName, CoverageStoreInfo.class)
                .getId();
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getStore(storeId, CoverageStoreInfo.class);

        GridCoverage2DReader reader = (GridCoverage2DReader) catalog.getResourcePool()
                .getGridCoverageReader(store, null);
        String[] coverageNames = reader.getGridCoverageNames();
        if (availableCoverages == null) {
            availableCoverages = new ArrayList<String>();
        }
        for (String coverage : coverageNames) {
            ImageLayout layout = reader.getImageLayout(coverage);
            SampleModel sampleModel = layout.getSampleModel(null);
            final int numBands = sampleModel.getNumBands();
            for (int i = 0; i < numBands; i++) {
                availableCoverages.add(coverage + CoverageView.BAND_SEPARATOR + i);
            }
        }
        Collections.sort(availableCoverages);
        name = COVERAGE_VIEW_NAME;
        if (coverageName != null) {
            newCoverage = false;

            // grab the coverage view
            coverageViewInfo = coverageInfo != null ? coverageInfo : catalog.getResourceByStore(
                    store, coverageName, CoverageInfo.class);
            CoverageView coverageView = coverageViewInfo.getMetadata().get(
                    CoverageView.COVERAGE_VIEW, CoverageView.class);
            // the type can be still not saved
            if (coverageViewInfo != null) {
                coverageInfoId = coverageViewInfo.getId();
            }
            if (coverageView == null) {
                throw new IllegalArgumentException(
                        "The specified coverage does not have a coverage view attached to it");
            }
            outputBands = new ArrayList<CoverageBand>(coverageView.getCoverageBands());
            name = coverageView.getName();
        } else {
            outputBands = new ArrayList<CoverageBand>();
            newCoverage = true;
            coverageViewInfo = null;
        }
        selectedCoverages = new ArrayList<String>(availableCoverages);

        // build the form and the text area
        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);

        final TextField nameField = new TextField("name");
        nameField.setRequired(true);
        nameField.add(new CoverageViewNameValidator());
        form.add(nameField);

        coverageEditor = new CoverageViewEditor("coverages", new PropertyModel(this,
                "selectedCoverages"), new PropertyModel(this, "outputBands"), availableCoverages);
        form.add(coverageEditor);

        // save and cancel at the bottom of the page
        form.add(new SubmitLink("save") {
            @Override
            public void onSubmit() {
                onSave();
            }
        });
        form.add(new Link("cancel") {

            @Override
            public void onClick() {
                onCancel();
            }
        });
    }


    protected CoverageView buildCoverageView() throws IOException {
        return new CoverageView(name, coverageEditor.currentOutputBands);
    }

    /**
     * Data stores tend to return IOExceptions with no explanation, and the actual error coming from the db is in the cause. This method extracts the
     * first not null message in the cause chain
     * 
     * @param t
     * @return
     */
    protected String getFirstErrorMessage(Throwable t) {
        Throwable original = t;

        while (!(t instanceof SQLException)) {
            t = t.getCause();
            if (t == null) {
                break;
            }
        }

        if (t == null) {
            return original.getMessage();
        } else {
            return t.getMessage();
        }
    }

    protected abstract void onSave();

    protected abstract void onCancel();

    /**
     * Checks the {@link CoverageView} name is unique
     */
    class CoverageViewNameValidator extends AbstractValidator {
        @Override
        protected void onValidate(IValidatable validatable) {
            String vcName = (String) validatable.getValue();

            final CoverageStoreInfo store = getCatalog().getStore(storeId, CoverageStoreInfo.class);
            List<CoverageInfo> coverages = getCatalog().getCoveragesByCoverageStore(store);
            for (CoverageInfo curr : coverages) {
                CoverageView currvc = curr.getMetadata().get(CoverageView.COVERAGE_VIEW, CoverageView.class);
                if (currvc != null) {
                    if (coverageInfoId == null || !coverageInfoId.equals(curr.getId())) {
                        if (currvc.getName().equals(vcName) && newCoverage) {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("name", vcName);
                            map.put("coverageName", curr.getName());
                            error(validatable, "duplicateCoverageViewName", map);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    public List<String> getSelectedCoverages() {
        return selectedCoverages;
    }

    public void setSelectedCoverages(List<String> selectedCoverages) {
        this.selectedCoverages = selectedCoverages;
    }

    private class CompositionTypeRenderer implements IChoiceRenderer {

        public CompositionTypeRenderer() {
        }

        public Object getDisplayValue(Object object) {
            return object.toString();
        }

        public String getIdValue(Object object, int index) {
            return object.toString();
        }
    }

}
