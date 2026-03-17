package com.mockhub.venue.entity;

import java.math.BigDecimal;

import com.mockhub.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"row_id", "seat_number"})
})
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "row_id", nullable = false)
    private SeatRow row;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "seat_type", nullable = false, length = 30)
    private String seatType;

    @Column(name = "svg_x", precision = 8, scale = 2)
    private BigDecimal svgX;

    @Column(name = "svg_y", precision = 8, scale = 2)
    private BigDecimal svgY;

    @Column(name = "is_aisle", nullable = false)
    private boolean isAisle;

    public Seat() {
    }

    public SeatRow getRow() {
        return row;
    }

    public void setRow(SeatRow row) {
        this.row = row;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getSvgX() {
        return svgX;
    }

    public void setSvgX(BigDecimal svgX) {
        this.svgX = svgX;
    }

    public BigDecimal getSvgY() {
        return svgY;
    }

    public void setSvgY(BigDecimal svgY) {
        this.svgY = svgY;
    }

    public boolean isAisle() {
        return isAisle;
    }

    public void setAisle(boolean aisle) {
        this.isAisle = aisle;
    }
}
