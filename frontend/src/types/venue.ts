export interface VenueSummary {
  id: number;
  name: string;
  slug: string;
  city: string;
  state: string;
  venueType: string;
  capacity: number;
  imageUrl: string | null;
}

export interface VenueDetail extends VenueSummary {
  addressLine1: string;
  addressLine2: string | null;
  zipCode: string;
  country: string;
  sections: SectionInfo[];
}

export interface SectionInfo {
  id: number;
  name: string;
  sectionType: string;
  capacity: number;
}
