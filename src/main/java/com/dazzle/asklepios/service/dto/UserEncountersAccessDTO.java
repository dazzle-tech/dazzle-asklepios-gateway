package com.dazzle.asklepios.service.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserEncountersAccessDTO implements Serializable {

    private boolean allowOngoingVisit;
    private boolean canUnDischargeUrgentCare;
    private boolean canUnCompleteEncounter;
    private boolean canReturnClaimForResubmit;

    public UserEncountersAccessDTO() {
    }

    public UserEncountersAccessDTO(
        boolean allowOngoingVisit,
        boolean canUnDischargeUrgentCare,
        boolean canUnCompleteEncounter,
        boolean canReturnClaimForResubmit
    ) {
        this.allowOngoingVisit = allowOngoingVisit;
        this.canUnDischargeUrgentCare = canUnDischargeUrgentCare;
        this.canUnCompleteEncounter = canUnCompleteEncounter;
        this.canReturnClaimForResubmit = canReturnClaimForResubmit;
    }
}
