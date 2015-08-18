package org.fao.sola.admin.web.beans.helpers;

import java.util.Comparator;
import org.sola.admin.opentenure.services.ejbs.claim.entities.FieldTemplate;

/**
 * Allows to sort array of {@link FieldTemplate} entity by item order.
 */
public class FormFieldSorter implements Comparator<FieldTemplate>{

    @Override
    public int compare(FieldTemplate f1, FieldTemplate f2) {
        return Integer.compare(f1.getItemOrder(), f2.getItemOrder());
    }
}