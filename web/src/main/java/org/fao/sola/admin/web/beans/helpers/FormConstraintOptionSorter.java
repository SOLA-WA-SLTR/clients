package org.fao.sola.admin.web.beans.helpers;

import java.util.Comparator;
import org.sola.admin.opentenure.services.ejbs.claim.entities.FieldConstraintOption;

/**
 * Allows to sort array of {@link FieldConstraintOption} entity by item order.
 */
public class FormConstraintOptionSorter implements Comparator<FieldConstraintOption>{

    @Override
    public int compare(FieldConstraintOption fco1, FieldConstraintOption fco2) {
        return Integer.compare(fco1.getItemOrder(), fco2.getItemOrder());
    }
}