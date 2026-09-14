package com.nemblex.repository;

import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;

public interface TicketStatusPriorityCount {

    TicketStatus getStatus();

    TicketPriority getPriority();

    long getCount();
}
