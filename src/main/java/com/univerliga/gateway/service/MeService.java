package com.univerliga.gateway.service;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.dto.MeResponse;
import com.univerliga.gateway.security.CurrentUser;
import com.univerliga.gateway.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class MeService {
    private final CurrentUserService currentUserService;
    private final CrmClient crmClient;

    public MeService(CurrentUserService currentUserService, CrmClient crmClient) {
        this.currentUserService = currentUserService;
        this.crmClient = crmClient;
    }

    public MeResponse me() {
        CurrentUser user = currentUserService.getCurrentUser();
        return crmClient.findPersonById(user.personId())
            .map(p -> new MeResponse(p.id(), user.username(), user.roles().stream().sorted().toList(), p.departmentId(), p.teamId(), p.displayName()))
            .orElseGet(() -> new MeResponse(user.personId(), user.username(), user.roles().stream().sorted(Comparator.naturalOrder()).toList(), "d_10", "t_5", "User"));
    }
}
