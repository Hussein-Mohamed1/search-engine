"use client";
import { redirect, useSearchParams } from "next/navigation";
import useSWR from "swr";
import { useEffect, useState } from "react";
import { SearchBar } from "@/components/searchBar";
import Link from "next/link";
import SearchResults from "@/components/searchResults";
import { Pagination } from "@/components/pagination";

const fetcher = (...args) => fetch(...args).then((res) => res.json());

export default function Page() {
  const searchParams = useSearchParams();
  if (!searchParams.has("q") || searchParams.get("q").trim() === "")
    redirect("/");

  const { data, error, isLoading } = useSWR(
    `/api/query?${searchParams.toString()}`,
    fetcher
  );

  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div className="flex flex-col space-y-8 min-h-screen">
      {/* Search Bar (Smoothly Transitions on Scroll) */}
      <div
        className={`sticky flex flex-row items-center justify-center mx-auto px-8 z-10 transition-all duration-300 rounded-2xl backdrop-blur-lg bg-white/10 ${
          scrolled ? "py-3 shadow-md top-1" : "py-4 top-2"
        }`}
      >
        <Link href="/">
          <div className="tracking-widest text-2xl font-extrabold mr-4">
            Lumos
          </div>
        </Link>
        <SearchBar
          className="items-center transition-all duration-300"
          height={scrolled ? "h-10" : "h-12"}
          width={scrolled ? "w-[30em]" : "w-[40em]"}
          variant={scrolled ? "minimized" : "search"}
        />
      </div>

      {/* Search results */}
      {isLoading && (
        <div className="flex justify-center py-8">
          <div className="animate-pulse text-white">Loading results...</div>
        </div>
      )}
      {error && (
        <div className="text-red-500">
          Error loading results. Please try again.
        </div>
      )}
      {!isLoading && !error && (
        <>
          <SearchResults data={data?.results} className="mx-14 w-[50%]" />
          {/* Pagination Section */}
          <div className="flex mx-auto mb-8">
            <Pagination pagesNum={data.pages} />
          </div>
        </>
      )}
    </div>
  );
}
