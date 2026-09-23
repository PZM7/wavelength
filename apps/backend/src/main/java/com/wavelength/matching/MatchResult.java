package com.wavelength.matching;

import com.wavelength.users.PublicUser;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"compatibility", "reasons", "user"})
public record MatchResult(PublicUser user, double compatibility, List<MatchReason> reasons) {}
