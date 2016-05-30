/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.CoverageView.CoverageBand;

/**
 *
 */
@SuppressWarnings("serial")
public class CoverageViewEditor extends FormComponentPanel {

    IModel coverages;
    IModel outputBands;
    List<String> availableCoverages; 
    List<CoverageBand> currentOutputBands;
    ListMultipleChoice<String> coveragesChoice;
    CompositionType compositionType;

    ListMultipleChoice<CoverageBand> outputBandsChoice;

    TextField definition;
    DropDownChoice<CompositionType> compositionChoice;

    /**
     * Creates a new editor.
     * 
     * @param id
     * @param The module should return a non null collection of strings.
     */
    public CoverageViewEditor(String id, final IModel inputCoverages, final IModel bands,
            List<String> availableCoverages) {
        super(id, inputCoverages);
        this.coverages = inputCoverages;
        this.outputBands = bands;

        this.availableCoverages = availableCoverages;

        coveragesChoice = new ListMultipleChoice<String>("coveragesChoice", new Model(),
                new ArrayList<String>((List<String>) coverages.getObject()),
                new ChoiceRenderer<String>() {
                    @Override
                    public Object getDisplayValue(String coverage) {
                        return coverage;
                    }
                });
        coveragesChoice.setOutputMarkupId(true);
        add(coveragesChoice);

        new ArrayList<CoverageBand>();
        outputBandsChoice = new ListMultipleChoice<CoverageBand>("outputBandsChoice", new Model(),
                new ArrayList<CoverageBand>((List<CoverageBand>) outputBands.getObject()),
                new ChoiceRenderer<CoverageBand>() {
                    @Override
                    public Object getDisplayValue(CoverageBand vcb) {
                        return vcb.getDefinition();
                    }
                });
        outputBandsChoice.setOutputMarkupId(true);
        add(outputBandsChoice);

        currentOutputBands = new ArrayList<CoverageBand>(outputBandsChoice.getChoices());

        add(addBandButton());
        definition = new TextField("definition", new Model());
        definition.setOutputMarkupId(true);

        // TODO: make this parametric on the CompositionType choice
        definition.setEnabled(false);
        // TODO Uncomment this row when it can be used
        //add(definition);
        compositionType = CompositionType.getDefault();
        compositionChoice = new DropDownChoice("compositionType", new PropertyModel(this,
                "compositionType"), Arrays.asList(CompositionType.BAND_SELECT), new CompositionTypeRenderer());

        compositionChoice.setOutputMarkupId(true);
        compositionChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                compositionType = compositionChoice.getModelObject();
                // TODO Uncomment these rows when they can be used
                //definition.setEnabled(compositionType != CompositionType.BAND_SELECT);
                //target.addComponent(definition);
            }
        });

        // TODO Uncomment this row when it can be used
        //add(compositionChoice);
        add(addRemoveAllButton());
        add(addRemoveButton());
    }

    private AjaxButton addBandButton() {
        AjaxButton button = new AjaxButton("addBand") {

            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                List<String> selection = (List<String>) coveragesChoice.getModelObject();
                compositionType = compositionChoice.getModelObject();
                List<CoverageBand> bandsList = new ArrayList<CoverageBand>();
                int i = currentOutputBands != null && !currentOutputBands.isEmpty() ? currentOutputBands.size() : 0;
                for (Iterator<String> it = selection.iterator(); it.hasNext();) {
                    String coverage = it.next();

                    final int bandIndexChar = coverage.indexOf(CoverageView.BAND_SEPARATOR);
                    String coverageName = coverage;
                    String bandIndex = "-1";
                    if (bandIndexChar != -1) {
                        coverageName = coverage.substring(0, bandIndexChar);
                        bandIndex = coverage.substring(bandIndexChar + 1, coverage.length());
                    }
                    CoverageBand band = new CoverageBand(
                            Collections.singletonList(new InputCoverageBand(coverageName, bandIndex)),
                            coverageName, i++, compositionType);
                    bandsList.add(band);

                }
                currentOutputBands.addAll(bandsList);
                outputBandsChoice.setChoices(currentOutputBands);
                outputBandsChoice.modelChanged();
                coveragesChoice.setChoices(availableCoverages);
                coveragesChoice.modelChanged();

                // TODO: Reset choice
                target.addComponent(coveragesChoice);
                target.addComponent(outputBandsChoice);
            }
        };
        return button;
    }

    private AjaxButton addRemoveAllButton() {
        AjaxButton button = new AjaxButton("removeAllBands") {

            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                List<CoverageBand> outputBands = (List<CoverageBand>) outputBandsChoice
                        .getModelObject();
                outputBands.clear();
                currentOutputBands.clear();
                outputBandsChoice.setChoices(currentOutputBands);
                outputBandsChoice.modelChanged();

                // TODO: Reset choice
                target.addComponent(outputBandsChoice);
            }
        };
        return button;
    }

    private AjaxButton addRemoveButton() {
        AjaxButton button = new AjaxButton("removeBands") {

            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {

                List<CoverageBand> removedBands = (List<CoverageBand>) outputBandsChoice
                        .getModel().getObject();

                for (Object band : removedBands) {
                    currentOutputBands.remove(band);
                }

                outputBandsChoice.setChoices(currentOutputBands);
                outputBandsChoice.modelChanged();

                // TODO: Reset choice
                target.addComponent(outputBandsChoice);
            }
        };
        return button;
    }

    private class CompositionTypeRenderer implements IChoiceRenderer {

        public CompositionTypeRenderer() {
        }

        public Object getDisplayValue(Object object) {
            return ((CompositionType) object).displayValue();
        }

        public String getIdValue(Object object, int index) {
            return ((CompositionType) object).toValue();
        }
    }

}
