package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QureyData {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String selectedValue1;
    private String selectedValue2;
}
