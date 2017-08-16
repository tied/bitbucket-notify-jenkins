package com.mastercard.scm.bitbucket.notifyjenkins;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JenkinsResponse {
    private int code;
    private String body;
}
