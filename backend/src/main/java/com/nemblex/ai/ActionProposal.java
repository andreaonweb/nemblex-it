package com.nemblex.ai;

import com.nemblex.entity.enums.TicketAction;

public record ActionProposal(TicketAction action, String reason, String employeeMessage) {
}
