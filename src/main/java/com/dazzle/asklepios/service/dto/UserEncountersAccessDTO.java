package com.dazzle.asklepios.service.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserEncountersAccessDTO implements Serializable {

    private boolean allowOngoingVisit;
    private boolean canUnDischargeUrgentCare;
    private boolean canUnCompleteEncounter;
    private boolean canReturnClaimForResubmit;
    private boolean canCancelServiceBeforeInvoice;

    public UserEncountersAccessDTO() {
    }

    public UserEncountersAccessDTO(
        boolean allowOngoingVisit,
        boolean canUnDischargeUrgentCare,
        boolean canUnCompleteEncounter,
        boolean canReturnClaimForResubmit,
        boolean canCancelServiceBeforeInvoice
    ) {
        this.allowOngoingVisit = allowOngoingVisit;
        this.canUnDischargeUrgentCare = canUnDischargeUrgentCare;
        this.canUnCompleteEncounter = canUnCompleteEncounter;
        this.canReturnClaimForResubmit = canReturnClaimForResubmit;
        this.canCancelServiceBeforeInvoice = canCancelServiceBeforeInvoice;
    }
}
