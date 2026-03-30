# Search Events

Guided event search for MockHub. Use this skill when the user wants to find events or tickets on MockHub.

## When to use

- User asks to find events, concerts, shows, or tickets
- User wants to browse what's available
- User asks about specific artists, venues, or cities
- Before any purchase flow — find the right event first

## Search strategy

Do NOT call findTickets with broad or empty filters. Instead, guide the user through a focused search:

### Step 1: Gather context

Ask the user at least one of these before searching:
- **What** are you looking for? (artist name, event type, genre)
- **Where?** (city — MockHub has events in New York, Chicago, Los Angeles, Boston, Brooklyn, Inglewood)
- **When?** (date range — events run from April through August 2026)
- **Budget?** (price range)
- **Category?** (concerts, sports, theater, comedy, festivals)

If the user gives a vague request like "find me some tickets", ask: "What kind of event are you interested in? I can search by artist, city, category, or date."

### Step 2: Search with filters

Call the MockHub API to search. Use one of these approaches:

**Via MCP (if connected):**
Use the `findTickets` tool with specific filters. Always include at least one of: query, category, city.

**Via REST API:**
```
GET https://mockhub.kousenit.com/api/v1/events?q={query}&category={slug}&city={city}&page=0&size=10
```

Categories: `concerts`, `sports`, `theater`, `comedy`, `festivals`
Cities with events: New York, Chicago, Los Angeles, Boston, Brooklyn, Inglewood

### Step 3: Present results

For each matching event, show:
- Event name and artist
- Date and venue (city)
- Price range (from min_price)
- Tickets available

Format as a concise list. If more than 5 results, show the top 5 and mention how many more are available.

### Step 4: Drill deeper

After showing results, offer:
- "Want to see available tickets for [event name]?"
- "Should I check prices for a specific section?"
- "Want to narrow by date or price range?"

## Example conversation

User: "I want to go to a concert"
Agent: "I can help find concerts! A few questions to narrow it down:
- Any particular artist or genre you're interested in?
- Which city? We have events in NYC, Chicago, LA, Boston, and more.
- Any date preference?"

User: "Rock music in Chicago"
Agent: [calls findTickets with category=concerts, city=Chicago, query=rock]
"Here are the rock concerts in Chicago: ..."

## Notes

- The events database is seeded with well-known artists (Taylor Swift, Radiohead, Green Day, etc.)
- All events are in the April-August 2026 timeframe
- Prices are dynamic — they change based on supply, demand, and time to event
- Demo accounts: buyer@mockhub.com / buyer123
