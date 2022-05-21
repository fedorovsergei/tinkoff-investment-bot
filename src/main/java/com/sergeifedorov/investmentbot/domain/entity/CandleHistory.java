package com.sergeifedorov.investmentbot.domain.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "candle_history")
public class CandleHistory extends BaseAbstractEntityId {

    private LocalDateTime date;
    private String figi;
    private Long unit;
    private Integer nano;
}
