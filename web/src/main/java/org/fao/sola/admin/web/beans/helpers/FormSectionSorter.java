package org.fao.sola.admin.web.beans.helpers;

import java.util.Comparator;
import org.sola.admin.opentenure.services.ejbs.claim.entities.SectionTemplate;

/**
 * Allows to sort array of {@link SectionTemplate} entity by item order.
 */
public class FormSectionSorter implements Comparator<SectionTemplate>{

    @Override
    public int compare(SectionTemplate s1, SectionTemplate s2) {
        return Integer.compare(s1.getItemOrder(), s2.getItemOrder());
    }
}