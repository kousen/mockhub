package com.mockhub.venue.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.mockhub.common.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "seat_rows", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"section_id", "row_label"})
})
public class SeatRow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_count", nullable = false)
    private int seatCount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "svg_y_offset", precision = 8, scale = 2)
    private BigDecimal svgYOffset;

    @OneToMany(mappedBy = "row", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("seatNumber ASC")
    private List<Seat> seats = new ArrayList<>();

    public SeatRow() {
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public BigDecimal getSvgYOffset() {
        return svgYOffset;
    }

    public void setSvgYOffset(BigDecimal svgYOffset) {
        this.svgYOffset = svgYOffset;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }
}
