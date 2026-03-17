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
@Table(name = "sections", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"venue_id", "name"})
})
public class Section extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "section_type", nullable = false, length = 50)
    private String sectionType;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "svg_path_id", length = 100)
    private String svgPathId;

    @Column(name = "svg_x", precision = 8, scale = 2)
    private BigDecimal svgX;

    @Column(name = "svg_y", precision = 8, scale = 2)
    private BigDecimal svgY;

    @Column(name = "svg_width", precision = 8, scale = 2)
    private BigDecimal svgWidth;

    @Column(name = "svg_height", precision = 8, scale = 2)
    private BigDecimal svgHeight;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<SeatRow> seatRows = new ArrayList<>();

    public Section() {
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSvgPathId() {
        return svgPathId;
    }

    public void setSvgPathId(String svgPathId) {
        this.svgPathId = svgPathId;
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

    public BigDecimal getSvgWidth() {
        return svgWidth;
    }

    public void setSvgWidth(BigDecimal svgWidth) {
        this.svgWidth = svgWidth;
    }

    public BigDecimal getSvgHeight() {
        return svgHeight;
    }

    public void setSvgHeight(BigDecimal svgHeight) {
        this.svgHeight = svgHeight;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public List<SeatRow> getSeatRows() {
        return seatRows;
    }

    public void setSeatRows(List<SeatRow> seatRows) {
        this.seatRows = seatRows;
    }
}
