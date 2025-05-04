"use server";

import { NextResponse } from "next/server";

const MAX_SUGGESTIONS = 5;
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export async function GET(request) {
  try {
    const searchParams = request?.nextUrl.searchParams;
    const query = searchParams?.get("q");

    // Call the backend API
    const response = await fetch(`${API_BASE_URL}/suggestions?q=${encodeURIComponent(query || '')}`);

    if (!response.ok) {
      throw new Error('Failed to fetch suggestions');
    }

    const data = await response.json();

    // Return the suggestions from the backend
    return NextResponse.json(data.suggestions || [], {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  } catch (error) {
    console.error('Error fetching suggestions:', error);
    return NextResponse.json([], {
      status: 500,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }
}