"use client";
import { redirect, useSearchParams } from "next/navigation";
import useSWR from "swr";
import { useEffect, useRef, useState } from "react";
import { SearchBar } from "@/components/searchBar";
import Link from "next/link";
import SearchResults from "@/components/searchResults";
import LoadingIcons from "react-loading-icons";
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
    const handleScroll = () => setScrolled(window.scrollY >50);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const noResults =
    !isLoading &&
    !error &&
    (!data || !data.results || data.results.length === 0);

  return (
    <div className="flex flex-col space-y-8 my-22 md:my-8 min-h-screen">
      {/* Search Bar (Smoothly Transitions on Scroll) */}

      <div
        onMouseEnter={() => setScrolled(false)}
        onMouseLeave={() => setScrolled(window.scrollY > 1 ? true : false)}
        className={`fixed flex flex-row items-center justify-center mx-auto inset-x-0 w-fit px-8 z-[99999] transition-all duration-300 rounded-2xl backdrop-blur-lg bg-white/10 ${
          scrolled
            ? "py-3 shadow-md top-1 -translate-y-8 md:-translate-y-14 scale-85"
            : "py-4 top-1"
        }`}
      >
        <Link href="/">
          <div className="tracking-widest text-2xl font-extrabold mr-4">
            Lumos
          </div>
        </Link>
        <SearchBar
          className="items-center transition-all duration-300 w-52 md:w-[32em] h-10 "
          variant={scrolled ? "minimized" : "search"}
        />
      </div>

      {/* Loading indicator */}
      {isLoading && (
        <div className="flex flex-col items-center justify-center py-[20vh] max-w-xl mx-auto text-center">
          <div className="text-white">
            <LoadingIcons.Circles className="size-[8em]" />
          </div>
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="flex flex-col items-center justify-center py-16 px-4 max-w-xl mx-auto text-center">
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-8 backdrop-blur-sm">
            <svg
              className="w-16 h-16 text-red-500 mx-auto mb-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              ></path>
            </svg>
            <h3 className="text-xl font-bold text-red-500 mb-2">
              Search Error
            </h3>
            <p className="text-white/90 mb-4">
              We couldn&apos;t complete your search request. This might be due
              to a connection issue or a temporary problem with our search
              service.
            </p>
            <div className="flex flex-col space-y-3">
              <button
                onClick={() => window.location.reload()}
                className="bg-red-500 hover:bg-red-600 text-white font-medium py-2 px-4 rounded-lg transition-colors duration-300"
              >
                Try Again
              </button>
              <Link
                href="/"
                className="text-blue-400 hover:text-blue-300 transition-colors duration-300"
              >
                Return to Homepage
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* No results message */}
      {noResults && (
        <div className="flex flex-col items-center justify-center py-16 px-4 max-w-xl mx-auto text-center">
          <div className="bg-blue-500/10 border border-blue-500/30 rounded-xl p-8 backdrop-blur-sm">
            <svg
              className="w-16 h-16 text-blue-500 mx-auto mb-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              ></path>
            </svg>
            <h3 className="text-xl font-bold text-blue-500 mb-2">
              No Results Found
            </h3>
            <p className="text-white/90 mb-4">
              We couldn&apos;t find any matches for &quot;
              <span className="font-semibold">{searchParams.get("q")}</span>
              &quot;.
            </p>
            <div className="text-white/80 text-sm mb-6">
              <p className="mb-2">Suggestions:</p>
              <ul className="list-disc list-inside text-left space-y-1">
                <li>Check your spelling</li>
                <li>Try more general keywords</li>
                <li>Try different keywords</li>
                <li>Try fewer keywords</li>
              </ul>
            </div>
            <Link
              href="/"
              className="bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-4 rounded-lg transition-colors duration-300"
            >
              Return to Homepage
            </Link>
          </div>
        </div>
      )}

      {/* Search results */}
      {!isLoading &&
        !error &&
        data &&
        data.results &&
        data.results.length > 0 && (
          <>
            <SearchResults data={data.results} className="mx-8" />
            {/* Pagination Section */}
            <div className="flex mx-auto">
              <Pagination pagesNum={data.pages} />
            </div>
          </>
        )}
    </div>
  );
}
