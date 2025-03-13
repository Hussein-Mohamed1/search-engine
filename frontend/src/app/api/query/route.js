"use server";

import {NextResponse} from "next/server";

const searchResults = [{
    id: "w001",
    title: "TechWorld - Latest Technology News and Reviews",
    url: "https://www.techworld.com",
    description: "Stay updated with the latest technology news, gadget reviews, and industry insights. Covering everything from smartphones to AI advancements.",
    snippet: "...the newly released quantum computing framework promises to revolutionize how we approach complex calculations, making previously impossible tasks accessible to researchers...",
    dateIndexed: "2025-02-28",
    ranking: 0.95,
    category: "Technology",
    imageUrl: "https://placeholder.com/techworld-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 12548, comments: 876
    }
}, {
    id: "w002",
    title: "Culinary Creations - Recipes & Cooking Tips",
    url: "https://www.culinarycreations.com",
    description: "Discover delicious recipes, cooking techniques, and food inspiration from professional chefs and home cooks around the world.",
    snippet: "...this Mediterranean diet plan incorporates fresh vegetables, lean proteins, and heart-healthy fats to create balanced meals that are both nutritious and satisfying...",
    dateIndexed: "2025-03-05",
    ranking: 0.89,
    category: "Food",
    imageUrl: "https://placeholder.com/culinary-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 8932, comments: 1204
    }
}, {
    id: "w003",
    title: "Global Insights - International News & Analysis",
    url: "https://www.globalinsights.org",
    description: "In-depth analysis of global events, geopolitics, and international relations from expert journalists and analysts.",
    snippet: "...diplomatic tensions continue to rise as negotiations over climate accord agreements reach a critical phase, with developing nations demanding more support from industrialized countries...",
    dateIndexed: "2025-03-10",
    ranking: 0.92,
    category: "News",
    imageUrl: "https://placeholder.com/globalinsights-logo.png",
    hasVideo: false,
    socialMetrics: {
        shares: 15872, comments: 2341
    }
}, {
    id: "w004",
    title: "Health Horizon - Wellness & Medical Information",
    url: "https://www.healthhorizon.med",
    description: "Evidence-based health information, medical research updates, and wellness advice from healthcare professionals.",
    snippet: "...the groundbreaking study reveals new connections between gut microbiome health and cognitive function, suggesting dietary interventions may play a role in preventing cognitive decline...",
    dateIndexed: "2025-03-01",
    ranking: 0.91,
    category: "Health",
    imageUrl: "https://placeholder.com/healthhorizon-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 7865, comments: 932
    }
}, {
    id: "w005",
    title: "EcoSolutions - Environmental News & Sustainability",
    url: "https://www.ecosolutions.green",
    description: "Covering environmental issues, sustainability innovations, and climate change solutions for a greener future.",
    snippet: "...this breakthrough recycling technology can process previously unrecyclable plastic compounds, potentially eliminating millions of tons of waste from landfills annually...",
    dateIndexed: "2025-02-20",
    ranking: 0.87,
    category: "Environment",
    imageUrl: "https://placeholder.com/ecosolutions-logo.png",
    hasVideo: false,
    socialMetrics: {
        shares: 6238, comments: 745
    }
}, {
    id: "w006",
    title: "CodeCrafters - Programming Tutorials & Resources",
    url: "https://www.codecrafters.dev",
    description: "Learn programming languages, software development, and computer science concepts with interactive tutorials and projects.",
    snippet: "...our step-by-step guide walks you through building a scalable microservices architecture using containerization and orchestration tools...",
    dateIndexed: "2025-03-07",
    ranking: 0.94,
    category: "Technology",
    imageUrl: "https://placeholder.com/codecrafters-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 9124, comments: 1876
    }
}, {
    id: "w007",
    title: "Finance Focus - Investment News & Market Analysis",
    url: "https://www.financefocus.com",
    description: "Financial news, investment strategies, market analysis, and personal finance advice from industry experts.",
    snippet: "...analysts predict significant market volatility in response to emerging regulatory frameworks around cryptocurrency and decentralized finance platforms...",
    dateIndexed: "2025-03-09",
    ranking: 0.88,
    category: "Finance",
    imageUrl: "https://placeholder.com/financefocus-logo.png",
    hasVideo: false,
    socialMetrics: {
        shares: 5437, comments: 687
    }
}, {
    id: "w008",
    title: "Travel Treasures - Destinations & Travel Guides",
    url: "https://www.traveltreasures.world",
    description: "Discover travel destinations, itineraries, cultural experiences, and travel tips from seasoned explorers.",
    snippet: "...this hidden coastal village offers authentic cultural experiences and breathtaking views without the crowds of more popular tourist destinations in the region...",
    dateIndexed: "2025-02-25",
    ranking: 0.86,
    category: "Travel",
    imageUrl: "https://placeholder.com/traveltreasures-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 7892, comments: 1034
    }
}, {
    id: "w009",
    title: "Art Avenue - Creative Arts & Cultural Coverage",
    url: "https://www.artavenue.gallery",
    description: "Exploring visual arts, performing arts, cultural movements, and creative expression across different mediums.",
    snippet: "...the exhibition combines traditional techniques with augmented reality elements, creating an immersive experience that challenges viewers' perceptions of space and form...",
    dateIndexed: "2025-03-03",
    ranking: 0.85,
    category: "Arts",
    imageUrl: "https://placeholder.com/artavenue-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 4231, comments: 578
    }
}, {
    id: "w010",
    title: "Science Spectrum - Research & Scientific Discoveries",
    url: "https://www.sciencespectrum.edu",
    description: "Latest scientific research, breakthroughs, and explorations across all fields of science and technology.",
    snippet: "...this astronomical observation provides compelling evidence for the theoretical model of black hole formation in binary star systems that was proposed decades ago...",
    dateIndexed: "2025-03-08",
    ranking: 0.93,
    category: "Science",
    imageUrl: "https://placeholder.com/sciencespectrum-logo.png",
    hasVideo: false,
    socialMetrics: {
        shares: 11348, comments: 1453
    }
}, {
    id: "w011",
    title: "Education Excellence - Learning Resources & Academic Insights",
    url: "https://www.educationexcellence.org",
    description: "Educational resources, teaching methodologies, academic research, and learning innovations for students and educators.",
    snippet: "...this pedagogical approach incorporates real-world problem-solving challenges into mathematics curriculum, resulting in significantly higher student engagement and retention of concepts...",
    dateIndexed: "2025-02-22",
    ranking: 0.87,
    category: "Education",
    imageUrl: "https://placeholder.com/education-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 6429, comments: 892
    }
}, {
    id: "w012",
    title: "SportsZone - Athletic News & Analysis",
    url: "https://www.sportszone.com",
    description: "Sports news, game analysis, athlete profiles, and coverage of major sporting events from around the world.",
    snippet: "...the innovative training technique has been credited with the team's remarkable improvement in performance metrics and reduced injury rates over the competitive season...",
    dateIndexed: "2025-03-11",
    ranking: 0.89,
    category: "Sports",
    imageUrl: "https://placeholder.com/sportszone-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 9876, comments: 1762
    }
}, {
    id: "w013",
    title: "FutureTech - Emerging Technologies & Innovation",
    url: "https://www.futuretech.io",
    description: "Exploring cutting-edge technologies, innovation trends, and future-focused advancements across industries.",
    snippet: "...the neural interface breakthrough demonstrates unprecedented accuracy in translating brain signals into digital commands, opening new possibilities for assistive technologies...",
    dateIndexed: "2025-03-04",
    ranking: 0.95,
    category: "Technology",
    imageUrl: "https://placeholder.com/futuretech-logo.png",
    hasVideo: false,
    socialMetrics: {
        shares: 13572, comments: 2134
    }
}, {
    id: "w014",
    title: "Urban Living - City Life & Modern Housing",
    url: "https://www.urbanliving.design",
    description: "Urban lifestyle trends, modern housing solutions, city planning innovations, and community development.",
    snippet: "...this mixed-use development project integrates affordable housing, green spaces, and commercial areas in a way that promotes community interaction and sustainability...",
    dateIndexed: "2025-02-27",
    ranking: 0.84,
    category: "Lifestyle",
    imageUrl: "https://placeholder.com/urbanliving-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 5281, comments: 743
    }
}, {
    id: "w015",
    title: "Music Matters - Audio Reviews & Artist Spotlights",
    url: "https://www.musicmatters.audio",
    description: "Music reviews, artist interviews, industry trends, and audio equipment analysis for music enthusiasts.",
    snippet: "...the experimental album combines traditional orchestral arrangements with electronic elements to create a soundscape that defies conventional genre classifications...",
    dateIndexed: "2025-03-02",
    ranking: 0.86,
    category: "Entertainment",
    imageUrl: "https://placeholder.com/musicmatters-logo.png",
    hasVideo: true,
    socialMetrics: {
        shares: 7219, comments: 1382
    }
}];


export async function GET() {
    // sliced the response for pagination later
    return NextResponse.json(searchResults.slice(0, 10), {});
}