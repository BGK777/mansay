package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditUserDto {
    private String nickName;
    private String introduce;
    private Boolean gender;
    private String city;
    private LocalDate birthday;
}
