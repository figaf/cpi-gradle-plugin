package com.figaf.plugin.entities;

import lombok.*;

/**
 * @author Arsenii Istlentev
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(of = {"protocol", "host", "port", "username"})
public class Agent {

    private String protocol;
    private String host;
    private Integer port;
    private String username;
    private String password;
}
