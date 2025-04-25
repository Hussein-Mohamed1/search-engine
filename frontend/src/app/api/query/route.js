"use server";

import { NextResponse } from "next/server";
const RESULTS_PER_PAGE = 10;

export async function GET(request) {
  const searchParams = request?.nextUrl.searchParams;
  const query = searchParams?.get("q");
  const crtPage = searchParams?.get("page") || 0;

  try {
    const res = await fetch(
      `${process.env.BACKEND_URL}/api/search?q=${encodeURIComponent(query)}&page=${crtPage - 1 >= 0 ? crtPage - 1 : 0}&size=${RESULTS_PER_PAGE}`
    );
    if (!res.ok) {
      return NextResponse.json({ status: res.status, message: "Backend error" }, { status: res.status });
    }
    const data = await res.json();
    return NextResponse.json(
      {
        results: data.results ?? [],
        pages: data.pages ?? 0,
      },
      {}
    );
  } catch (e) {
    return NextResponse.json({ status: 500, message: `Something Went Wrong! ${e}` }, { status: 500 });
  }
}
