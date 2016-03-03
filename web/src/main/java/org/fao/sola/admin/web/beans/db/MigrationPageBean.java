package org.fao.sola.admin.web.beans.db;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.ibatis.exceptions.PersistenceException;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.sola.admin.opentenure.services.ejbs.claim.businesslogic.ClaimAdminEJBLocal;
import org.sola.common.ClaimStatusConstants;
import org.sola.common.StringUtility;
import org.sola.common.logging.LogUtility;
import org.sola.cs.services.ejb.search.businesslogic.SearchCSEJBLocal;
import org.sola.cs.services.ejb.search.repository.entities.ClaimSearchParams;
import org.sola.cs.services.ejb.search.repository.entities.ClaimSearchResult;
import org.sola.cs.services.ejbs.claim.businesslogic.ClaimEJBLocal;
import org.sola.cs.services.ejbs.claim.entities.Attachment;
import org.sola.cs.services.ejbs.claim.entities.AttachmentBinary;
import org.sola.cs.services.ejbs.claim.entities.Claim;
import org.sola.cs.services.ejbs.claim.entities.ClaimParty;
import org.sola.cs.services.ejbs.claim.entities.ClaimShare;
import org.sola.cs.services.ejbs.claim.entities.FieldPayload;
import org.sola.cs.services.ejbs.claim.entities.FieldType;
import org.sola.cs.services.ejbs.claim.entities.SectionElementPayload;
import org.sola.cs.services.ejbs.claim.entities.SectionPayload;
import org.sola.services.common.EntityAction;
import org.sola.services.common.br.ValidationResult;
import org.sola.services.common.faults.FaultUtility;
import org.sola.services.common.faults.SOLAValidationException;
import org.sola.services.digitalarchive.businesslogic.DigitalArchiveEJBLocal;
import org.sola.services.digitalarchive.repository.entities.Document;
import org.sola.services.ejb.address.repository.entities.Address;
import org.sola.services.ejb.administrative.businesslogic.AdministrativeEJBLocal;
import org.sola.services.ejb.administrative.repository.entities.BaUnit;
import org.sola.services.ejb.administrative.repository.entities.BaUnitArea;
import org.sola.services.ejb.administrative.repository.entities.BaUnitNotation;
import org.sola.services.ejb.administrative.repository.entities.BaUnitOT;
import org.sola.services.ejb.administrative.repository.entities.ConditionForRrr;
import org.sola.services.ejb.administrative.repository.entities.Rrr;
import org.sola.services.ejb.administrative.repository.entities.RrrShare;
import org.sola.services.ejb.cadastre.businesslogic.CadastreEJBLocal;
import org.sola.services.ejb.cadastre.repository.entities.AddressForCadastreObject;
import org.sola.services.ejb.cadastre.repository.entities.CadastreObject;
import org.sola.services.ejb.cadastre.repository.entities.CadastreObjectOT;
import org.sola.services.ejb.cadastre.repository.entities.NewCadastreObjectIdentifier;
import org.sola.services.ejb.cadastre.repository.entities.SpatialValueArea;
import org.sola.services.ejb.party.repository.entities.Party;
import org.sola.services.ejb.source.repository.entities.Source;
import org.sola.services.ejb.system.br.Result;
import org.sola.services.ejb.system.businesslogic.SystemEJBLocal;
import org.sola.services.ejb.system.repository.entities.BrValidation;

/**
 * Contains methods to migrate claims into SOLA Registry tables
 */
@Named
@ViewScoped
public class MigrationPageBean extends AbstractBackingBean {

    @EJB
    SystemEJBLocal systemEJB;

    @EJB
    AdministrativeEJBLocal admEjb;

    @EJB
    SearchCSEJBLocal searchEjb;

    @EJB
    ClaimAdminEJBLocal claimAdminEjb;

    @EJB
    ClaimEJBLocal claimEjb;

    @EJB
    DigitalArchiveEJBLocal archiveEjb;

    @EJB
    CadastreEJBLocal cadEjb;

    private String log;

    private String parcelDuplicate;

    public String getLog() {
        return log;
    }

//    stateField    x
//    lgaField      x
//    wardField     x
//    blockField    ????
//    houseAddressNumberField   x
//    streetNameField           x
//    mapSheetField      ???  
//    occupantsField --- no  
//    capacityField --- no
//    [capacityList --- no]
//    otherCapacityField --- no
//    nigeriaNationalityField -- boolean   --> nationality      x
//    otherNationalityField ---> nationality                    x
//    shareTypeField  --  check
//    [shareTypeList] --  check
//    declarationOtherInterestField -- no bool
//    
//    evidenceSection -- No all section
//    otherRightsField --- other interests.... as it is cannot be used
    public void transfer() {
        // Get claims to transfer
        ClaimSearchParams params = new ClaimSearchParams();
        params.setStatusCode(ClaimStatusConstants.UNMODERATED);
        List<ClaimSearchResult> results = searchEjb.searchClaims(params);
        int claimsTotal = results.size();
        int claimsLoaded = 0;
        int claimsFailed = 0;
        log = "";

        // Iterate through claims
        for (final ClaimSearchResult claimSearch : results) {
            try {
                runUpdate(new Runnable() {
                    @Override
                    public void run() {
                        // Get claim
                        Claim claim = claimEjb.getClaim(claimSearch.getId());

                        // Create Application and populate
//                        Application application = new Application();
                        // Create BA unit and populate
                        BaUnitOT baUnit = new BaUnitOT();
                        baUnit.setRrrList(new ArrayList<Rrr>());
                        baUnit.setName(claim.getDescription());

                        // Dynamic form
                        String nameFirstPart = null;
                        String nameLastPart = null;
                        String location = "";
                        String state = "AN";
                        String nationality = "Nigeria";
                        String lga = "";
                        String ward = "";
                        String block = "";
                        String mapSheet = "";

                        BigDecimal baUnitSize = null;
//
                        // Cadastre object
                        BaUnitArea baUnitArea = new BaUnitArea();
                        baUnitArea.setBaUnitId(baUnit.getId());
                        baUnitArea.setTypeCode("officialArea");
                        CadastreObjectOT co = new CadastreObjectOT();
                        NewCadastreObjectIdentifier nco = new NewCadastreObjectIdentifier();
                        Address coAddress = new Address();
                        AddressForCadastreObject addressforco = new AddressForCadastreObject();
                        SpatialValueArea spa = new SpatialValueArea();

                        Result newNumberResult = null;

                        //
                        if (claim.getDynamicForm() != null) {

                            FieldPayload f = getFieldPayload(claim, "streetNameField");
                            if (f != null) {
                                location = getStringFieldValue(f);
                            }
                            f = getFieldPayload(claim, "houseAddressNumberField");
                            if (f != null) {
                                location = location + ", " + getStringFieldValue(f);
                            }

                            f = getFieldPayload(claim, "stateField");
                            if (f != null) {
                                state = getStringFieldValue(f);
                            }

                            f = getFieldPayload(claim, "lgaField");
                            if (f != null) {
                                lga = getStringFieldValue(f);
                            }

                            f = getFieldPayload(claim, "wardField");
                            if (f != null) {
                                ward = getStringFieldValue(f);
                            }

                            f = getFieldPayload(claim, "otherNationalityField");
                            if (f != null) {
                                if (getStringFieldValue(f) != null && getStringFieldValue(f) != "") {
                                    nationality = getStringFieldValue(f);
                                }
                            }

                            f = getFieldPayload(claim, "areaCofOhectares");
                            if (f != null) {
                                if (f.getBigDecimalPayload() != null) {
                                    baUnitSize = f.getBigDecimalPayload().multiply(BigDecimal.valueOf(10000));
                                }
                            }

//                          f = getFieldPayload(claim, "blockField");
//                            if (f != null) {
//                                block = getStringFieldValue(f);
//                            }
//                          f = getFieldPayload(claim, "mapSheetField");
//                            if (f != null) {
//                                mapSheet = getStringFieldValue(f);
//                            }
                        }

                        if (baUnitSize != null) {
                            baUnitArea.setSize(baUnitSize);
                        } else {
                            baUnitArea.setSize(BigDecimal.ZERO);
                        }

                        if (claim.getMappedGeometry() != null && claim.getMappedGeometry().length() > 0) {
                            // Add geometry
                            WKTReader wktReader = new WKTReader();
                            Geometry geom;
                            Geometry geomwithsrid;
                            try {
                                geom = wktReader.read(claim.getMappedGeometry());
//                                if (baUnitSize == null) {
                                baUnitArea.setTypeCode("calculatedArea");
                                baUnitArea.setSize(BigDecimal.valueOf(geom.getArea()));
//                                }
                                geom.setSRID(32632);

                                WKBWriter wkbWriter = new WKBWriter();

                                co.setGeomPolygon(wkbWriter.write(geom));

                                nco = cadEjb.getNewCadastreObjectIdentifier(co.getGeomPolygon(), "mapped_geometry");

                                nameFirstPart = nco.getFirstPart();
                                nameLastPart = nco.getLastPart();

                                spa.setSpatialUnitId(co.getId());
                                spa.setTypeCode("officialArea");
                                spa.setSize(BigDecimal.valueOf(geom.getArea()));
                                co.setSpatialValueAreaList(new ArrayList<SpatialValueArea>());
                                co.getSpatialValueAreaList().add(spa);
                            } catch (ParseException ex) {
                                throw new RuntimeException(ex);
                            }
                        }

                        if (!StringUtility.isEmpty(location)) {
                            baUnit.setLocation(location);
                        }

                        if (!StringUtility.isEmpty(claim.getLandUseCode())) {
                            baUnit.setLandUse(claim.getLandUseCode());
                        }

                        if (!StringUtility.isEmpty(nameLastPart)) {
                            baUnit.setNameLastpart(nameLastPart);
                            co.setNameLastpart(nameLastPart);
                        } else {
                            baUnit.setNameLastpart(state + '/' + lga + '/' + ward);
                            co.setNameLastpart(state + '/' + lga + '/' + ward);
                        }
                        if (!StringUtility.isEmpty(nameFirstPart)) {
                            baUnit.setNameFirstpart(nameFirstPart);
                            co.setNameFirstpart(nameFirstPart);
                        } else {
                            String result = "";
                            HashMap<String, Serializable> params = new HashMap<String, Serializable>();
                            params = new HashMap<String, Serializable>();
                            params.put("last_part", state + '/' + lga + '/' + ward);
                            params.put("cadastre_object_type", "parcel");
                            newNumberResult = systemEJB.checkRuleGetResultSingle(
                                    "generate-cadastre-object-firstpart", params);
                            if (newNumberResult != null && newNumberResult.getValue() != null) {
                                result = newNumberResult.getValue().toString();
                                baUnit.setNameFirstpart(result);
                                co.setNameFirstpart(result);
                            }
                        }

                        if (!StringUtility.isEmpty(claim.getLandUseCode())) {
                            baUnit.setLandUse(claim.getLandUseCode());
                        }

                        List<CadastreObjectOT> coExists = cadEjb.getCadastreObjectOTByAllParts(co.getNameFirstpart() + ' ' + co.getNameLastpart());
                        String baUnitList = "";
                        if (coExists.size() == 0) {
                            baUnit.setCadastreObjectList(new ArrayList<CadastreObjectOT>());
                            baUnit.getCadastreObjectList().add(co);
                        } else {

                            List<BaUnit> baUnitExists = admEjb.getBaUnitsByCadObject(coExists.get(0).getId());
                            for (BaUnit existingBaUnit : baUnitExists) {
                                baUnitList += existingBaUnit.getNameFirstpart() + "/" + existingBaUnit.getNameLastpart() + "\r\n";
                            }
                            if (baUnitExists.size() > 0) {
                                parcelDuplicate = " !! WARNING There are already ba units linked to the same parcel\r\n";
                                parcelDuplicate += " Take note of the following information and fix inconstistencies in SOLA SLTR\r\n";
                                parcelDuplicate += "Ba unit(s) [Volume/Folio]:\r\n" + baUnitList;
                                parcelDuplicate += "Parcel: plot " + co.getNameFirstpart() + "/" + co.getNameLastpart() + "\r\n";
                                parcelDuplicate += "===============================================\r\n";
                            }
                            baUnit.setCadastreObjectList(new ArrayList<CadastreObjectOT>());
                            baUnit.getCadastreObjectList().add(coExists.get(0));
                        }

                        // Set BaUnit name
                        if (StringUtility.isEmpty(baUnit.getName())) {
                            if (!StringUtility.isEmpty(baUnit.getNameFirstpart())
                                    || !StringUtility.isEmpty(baUnit.getNameLastpart())) {
                                baUnit.setName(StringUtility.empty(baUnit.getNameFirstpart()) + "\\"
                                        + StringUtility.empty(baUnit.getNameLastpart()));
                            }
                        }

                        if (StringUtility.isEmpty(baUnit.getName())) {
                            baUnit.setName("BaUnit");
                        }

                        // Rrr
                        Rrr rrr = new Rrr();
                        rrr.setBaUnitId(baUnit.getId());
                        rrr.setPrimary(true);

//                        if (!StringUtility.isEmpty(claim.getTypeCode())) {
//                            rrr.setTypeCode(claim.getTypeCode());
//                        } else {
                        rrr.setTypeCode("ownership");
//                        }
//
                        rrr.setRrrShareList(new ArrayList<RrrShare>());

                        BaUnitNotation notation = new BaUnitNotation();
                        notation.setRrrId(baUnit.getId());
                        notation.setNotationText(rrr.getTypeCode());
                        rrr.setNotation(notation);

                        if (claim.getShares() != null) {
                            for (ClaimShare claimShare : claim.getShares()) {
                                // Calculate denominator
                                short denominator = 1;
                                if (claimShare.getPercentage() != 0) {
                                    denominator = (short) (100 / claimShare.getPercentage());
                                }
                                RrrShare rrrShare = new RrrShare();
                                rrrShare.setRrrId(rrr.getId());
                                rrrShare.setNominator((short) 1);
                                rrrShare.setDenominator(denominator);
                                rrrShare.setRightHolderList(new ArrayList<Party>());
                                if (claimShare.getOwners() != null) {
                                    for (ClaimParty claimParty : claimShare.getOwners()) {
                                        Party party = new Party();
                                        if (!StringUtility.isEmpty(claimParty.getAddress())) {
                                            Address address = new Address();
                                            address.setDescription(claimParty.getAddress());
                                            party.setAddress(address);
                                        }
                                        party.setDob(claimParty.getBirthDate());
                                        party.setEmail(claimParty.getEmail());
                                        party.setState(state);
                                        party.setNationality(nationality);
                                        party.setGenderCode(claimParty.getGenderCode());
                                        party.setIdNumber(claimParty.getIdNumber());
                                        party.setIdTypeCode(claimParty.getIdTypeCode());
                                        party.setLastName(claimParty.getLastName());
                                        party.setMobile(claimParty.getMobilePhone());
                                        party.setName(claimParty.getName());
                                        party.setPhone(claimParty.getPhone());
                                        if (claimParty.isPerson()) {
                                            party.setTypeCode("naturalPerson");
                                        } else {
                                            party.setTypeCode("nonNaturalPerson");
                                        }
                                        rrrShare.getRightHolderList().add(party);
                                    }
                                }
                                rrr.getRrrShareList().add(rrrShare);
                            }
                        }

                        // Documents
                        rrr.setSourceList(new ArrayList<Source>());
                        baUnit.setSourceList(new ArrayList<Source>());

                        if (claim.getAttachments() != null) {
                            for (Attachment attachment : claim.getAttachments()) {
                                Source source = new Source();
                                AttachmentBinary attachBinary = claimEjb.getAttachment(attachment.getId());

                                if (attachBinary.getBody() != null && attachBinary.getBody().length > 0) {
                                    Document doc = new Document();
                                    doc.setBody(attachBinary.getBody());
                                    doc.setDescription(attachBinary.getDescription());
                                    doc.setExtension(attachBinary.getFileExtension());
                                    doc.setMimeType(attachBinary.getMimeType());
                                    archiveEjb.createDocument(doc);
                                    source.setArchiveDocumentId(doc.getId());
                                }

                                source.setDescription(attachment.getDescription());
                                source.setRecordation(attachment.getDocumentDate());
                                source.setReferenceNr(attachment.getReferenceNr());
                                source.setTypeCode(attachment.getTypeCode());
                                source.setContent(claim.getDescription());
//                                // Add to RRR
                                rrr.getSourceList().add(source);
                            }
                        }

                        // Add rrr to BaUnit
                        baUnit.setRrrList(new ArrayList<Rrr>());
                        baUnit.getRrrList().add(rrr);

                        // Import BaUnit
                        if (admEjb.importBaUnit(baUnit)) {
                            // Save BaUnit calculated area
                            admEjb.createBaUnitArea(baUnit.getId(), baUnitArea);
                            // Change claim status
                            claimAdminEjb.changeClaimStatus(claim.getId(), ClaimStatusConstants.MODERATED);
                        }
                    }
                }
                );
                claimsLoaded += 1;

            } catch (Exception e) {
                LogUtility.log("Failed to load claim", e);
                claimsFailed += 1;
                log += String.format("Failed to load claim #%s with the follwoing error:\r\n", claimSearch.getNr());

                if (FaultUtility.hasCause(e, SOLAValidationException.class)) {
                    List<ValidationResult> validationResults = ((SOLAValidationException) FaultUtility
                            .getCause(e, SOLAValidationException.class))
                            .getValidationResultList();
                    for (ValidationResult result : validationResults) {
                        if (result.getSeverity().equals(BrValidation.SEVERITY_CRITICAL)) {
                            log += String.format("- %s\r\n", result.getFeedback());
                        }
                    }
                } else if (FaultUtility.hasCause(e, PersistenceException.class)) {
                    PersistenceException ex = FaultUtility.getCause(e, PersistenceException.class);
                    if (ex.getCause() != null) {
                        log += StringUtility.empty(ex.getCause().getMessage());
                    } else {
                        log += StringUtility.empty(ex.getMessage());
                    }
                } else {
                    log += LogUtility.getStackTraceAsString(e);
                }
                log += "\r\n";
            }
        }

        // Put summary on top of the log
        log = "===============================================\r\n" + log;

        if (claimsFailed > 0) {
            log = String.format("Failed %s claim(s)\r\n", claimsFailed) + log;
        }
        log = String.format("Loaded %s claim(s)\r\n", claimsLoaded) + log;
        log = String.format("Found %s claim(s)\r\n", claimsTotal) + log;
        if (parcelDuplicate != null) {
            log = "===============================================\r\n" + parcelDuplicate + log;
        }
    }

    private String getStringFieldValue(FieldPayload f) {
        if (FieldType.TYPE_DECIMAL.equalsIgnoreCase(f.getFieldType())) {
            if (f.getBigDecimalPayload() != null) {
                return f.getBigDecimalPayload().toPlainString();
            } else {
                return null;
            }
        } else if (FieldType.TYPE_BOOL.equalsIgnoreCase(f.getFieldType())) {
            return Boolean.toString(f.isBooleanPayload());
        } else if (FieldType.TYPE_INTEGER.equalsIgnoreCase(f.getFieldType())) {
            if (f.getBigDecimalPayload() != null) {
                return f.getBigDecimalPayload().toBigInteger().toString();
            } else {
                return null;
            }
        } else {
            return f.getStringPayload();
        }
    }

    private Date getDateFieldValue(String stringDate) throws java.text.ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String dateInString = stringDate;

        Date date = formatter.parse(dateInString);
        System.out.println(date);
        System.out.println(formatter.format(date));
        return date;

    }

    private FieldPayload getFieldPayload(Claim claim, String fieldName) {
        if (claim.getDynamicForm() == null || claim.getDynamicForm().getSectionPayloadList() == null
                || StringUtility.isEmpty(fieldName)) {
            return null;
        }

        for (SectionPayload section : claim.getDynamicForm().getSectionPayloadList()) {
            if (section.getSectionElementPayloadList() != null) {
                for (SectionElementPayload sectionElement : section.getSectionElementPayloadList()) {
                    if (sectionElement.getFieldPayloadList() != null) {
                        for (FieldPayload field : sectionElement.getFieldPayloadList()) {
                            if (field.getName().equalsIgnoreCase(fieldName)) {
                                return field;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getExceptionMessage(Throwable e) {
        if (e != null) {
            if (!StringUtility.isEmpty(e.getMessage())) {
                return e.getMessage();
            } else {
                return getExceptionMessage(e.getCause());
            }
        }
        return "Unknown exception";
    }
}
