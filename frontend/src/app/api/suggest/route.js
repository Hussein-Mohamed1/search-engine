"use server";

import { NextResponse } from "next/server";
const MAX_SUGGESTIONS = 5;
const suggestions = [
  "JavaScript",
  "Max",
  "Apple",
  "IDK",
  "I",
  "Just",
  "WANNA",
  "SLEEPPPP!!! PLEASE",
];

export async function GET(request) {
  // sliced the response for pagination later
  const searchParams = request?.nextUrl.searchParams;
  const query = searchParams?.get("q");

  return NextResponse.json(suggestions, {});
}
