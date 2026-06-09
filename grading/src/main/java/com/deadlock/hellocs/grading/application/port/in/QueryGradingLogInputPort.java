package com.deadlock.hellocs.grading.application.port.in;

import com.deadlock.hellocs.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingLogListResult;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingLogResult;
import jakarta.validation.Valid;

import java.util.List;

public interface QueryGradingLogInputPort {
    GradingLogResult getGradingLog(Long requesterId, @Valid GetGradingLogCommand command);
    GradingDetailLogResult getGradingDetailLog(Long requesterId, @Valid GetGradingDetailLogCommand command);
    List<GradingLogListResult> getGradingLogList(Long requesterId);
}
