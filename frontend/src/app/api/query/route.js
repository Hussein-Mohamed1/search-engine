"use server";

import {NextResponse} from "next/server";

const searchResults = [
    {
        "url": "https://www.example.com/article/javascript-frameworks",
        "title": "Top 10 JavaScript Frameworks in 2025",
        "description": "Explore the most popular JavaScript frameworks of 2025. This comprehensive guide covers React, Vue, Svelte, and emerging technologies that are reshaping web development."
    },
    {
        "url": "https://www.techjournal.org/web/frontend/modern-js",
        "title": "Modern JavaScript Development Practices",
        "description": "An in-depth look at modern JavaScript development workflows, build tools, and best practices. Learn how top companies structure their frontend codebases for maximum performance and developer experience."
    },
    {
        "url": "https://www.dev-resources.net/tutorials/advanced-react-patterns",
        "title": "Advanced React Design Patterns",
        "description": "Master complex React patterns like compound components, render props, and state machines. This tutorial series walks through real-world examples with code samples and interactive demos."
    },
    {
        "url": "https://www.webperf.guide/optimizing/javascript-bundles",
        "title": "Optimizing JavaScript Bundle Size",
        "description": "Learn practical techniques to reduce your JavaScript bundle size and improve loading performance. Includes code splitting strategies, tree shaking optimization, and modern bundler configurations."
    },
    {
        "url": "https://www.github.com/open-source/awesome-web-framework",
        "title": "Awesome Web Framework - GitHub Repository",
        "description": "A curated collection of resources, tools, and libraries for modern web development. Includes starter templates, component libraries, and performance optimization tools."
    },
    {
        "url": "https://www.stackoverflow.com/questions/75492123/understanding-react-server-components",
        "title": "Understanding React Server Components - Stack Overflow",
        "description": "Detailed answers explaining how React Server Components work, when to use them, and how they differ from traditional client components. Includes performance considerations and migration strategies."
    },
    {
        "url": "https://www.css-tricks.com/building-responsive-layouts/grid-vs-flexbox",
        "title": "CSS Grid vs Flexbox: Choosing the Right Layout",
        "description": "A comprehensive comparison of CSS Grid and Flexbox with practical examples. Learn which layout system to use for different UI requirements and how to combine them effectively."
    },
    {
        "url": "https://www.nextjs.org/docs/app/building-your-application/routing",
        "title": "Next.js App Router Documentation",
        "description": "Official documentation for Next.js App Router, covering file-based routing, layouts, loading states, error handling, and server components integration."
    }
];


export async function GET() {
    // sliced the response for pagination later
    return NextResponse.json(searchResults.slice(0, 10), {});
}