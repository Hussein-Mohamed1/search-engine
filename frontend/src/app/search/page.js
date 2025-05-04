"use client";
import { redirect, useSearchParams } from "next/navigation";
import useSWR from "swr";
import { useEffect, useState } from "react";
import { SearchBar } from "@/components/searchBar";
import Link from "next/link";
import SearchResults from "@/components/searchResults";
import LoadingIcons from "react-loading-icons";
import { Pagination } from "@/components/pagination";

// Enhanced fetcher with error handling
const fetcher = async (url) => {
  const res = await fetch(url);

  // If the response is not ok, throw an error
  if (!res.ok) {
    const error = new Error('An error occurred while fetching the data.');
    error.info = await res.json();
    error.status = res.status;
    throw error;
  }

  return res.json();
};

// Create a cache key generator for better cache management
const createCacheKey = (query, page) => {
  return `search:${query}:page:${page}`;
};

export default function Page() {
  const searchParams = useSearchParams();
  // Early return if no query
  if (!searchParams.has("q") || searchParams.get("q").trim() === "") {
    redirect("/");
  }

  const query = searchParams.get("q");
  const page = searchParams.get("page") || "1";
  const [scrolled, setScrolled] = useState(false);

  // Use the cache key for better cache management
  const { data, error, isLoading, mutate } = useSWR(
    `/api/query?${searchParams.toString()}`,
    fetcher,
    {
      // Configure SWR cache options
      revalidateOnFocus: false,
      revalidateOnReconnect: true,
      refreshWhenOffline: false,
      refreshWhenHidden: false,
      dedupingInterval: 5000,
      keepPreviousData: true,

      // Set a longer stale time - data remains fresh for 5 minutes
      focusThrottleInterval: 300000,


      // Callback when data is successfully fetched
      onSuccess: (data) => {
        // Store successful queries in localStorage for offline use
        try {
          const cacheKey = createCacheKey(query, page);
          const cachedQueriesJson = localStorage.getItem('cachedSearchQueries') || '{}';
          const cachedQueries = JSON.parse(cachedQueriesJson);

          // Store this result with timestamp
          cachedQueries[cacheKey] = {
            data,
            timestamp: Date.now()
          };

          // Limit cache size (optional)
          const cacheEntries = Object.entries(cachedQueries);
          if (cacheEntries.length > 50) { // Store max 50 queries
            // Sort by timestamp and keep most recent
            const sortedEntries = cacheEntries.sort((a, b) => b[1].timestamp - a[1].timestamp);
            const newCache = Object.fromEntries(sortedEntries.slice(0, 50));
            localStorage.setItem('cachedSearchQueries', JSON.stringify(newCache));
          } else {
            localStorage.setItem('cachedSearchQueries', JSON.stringify(cachedQueries));
          }
        } catch (err) {
          console.error("Error caching search results:", err);
        }
      }
    }
  );

  // Try to load from cache while waiting for fetch
  useEffect(() => {
    if (isLoading) {
      try {
        const cacheKey = createCacheKey(query, page);
        const cachedQueriesJson = localStorage.getItem('cachedSearchQueries') || '{}';
        const cachedQueries = JSON.parse(cachedQueriesJson);

        if (cachedQueries[cacheKey]) {
          const cachedData = cachedQueries[cacheKey].data;
          const cacheAge = Date.now() - cachedQueries[cacheKey].timestamp;

          // Use cache if it's less than 1 hour old
          if (cacheAge < 3600000) {
            // Use cached data while waiting for fresh data
            mutate(cachedData, false);
          }
        }
      } catch (err) {
        console.error("Error loading cached search results:", err);
      }
    }
  }, [query, page, isLoading, mutate]);

  // Scroll effect
  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Function to clear search cache (could be used for a "clear cache" button)
  const clearSearchCache = () => {
    try {
      localStorage.removeItem('cachedSearchQueries');
      // Force revalidation of current query
      mutate();
    } catch (err) {
      console.error("Error clearing search cache:", err);
    }
  };

  // Check for no results - make sure this is defined before using it
  const noResults = !isLoading &&
    !error &&
    (!data || !data.results || data.results.length === 0);

  return (
    <>
      {/* Loading Overlay - Full screen overlay with blur effect */}
      {isLoading && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-[100000] flex items-center justify-center">
          <div className="bg-blue-600/90 text-white px-6 py-4 rounded-xl shadow-xl flex flex-col items-center">
            <LoadingIcons.Circles className="size-16 mb-3" />
            <span className="text-lg font-medium">Updating results...</span>
          </div>
        </div>
      )}

      <div className={`flex flex-col space-y-8 my-22 md:my-8 min-h-screen ${isLoading ? 'pointer-events-none' : ''}`}>
        {/* Search Bar (Smoothly Transitions on Scroll) */}
        <div
          onMouseEnter={() => setScrolled(false)}
          onMouseLeave={() => setScrolled(window.scrollY > 1 ? true : false)}
          className={`fixed flex flex-row items-center justify-center mx-auto inset-x-0 w-fit px-8 z-[99999] transition-all duration-300 rounded-2xl backdrop-blur-lg bg-white/10 ${scrolled
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
            className="items-center transition-all duration-300 w-52 md:w-[32em] h-10"
            variant={scrolled ? "minimized" : "search"}
          />
        </div>

        {/* Loading indicator for initial load */}
        {isLoading && !data && (
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
                  onClick={() => mutate()}
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
                <span className="font-semibold">{query}</span>
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

        {/* Search results - show even during loading if we have cached data */}
        {data && data.results && data.results.length > 0 && (
          <div className={`transition-all duration-300 ${isLoading ? 'opacity-50 blur-sm' : 'opacity-100'}`}>
            <SearchResults data={data.results} stats={data.stats} className="mx-16" />

            {/* Pagination Section */}
            <div className="flex justify-center items-center">
              <Pagination pagesNum={data.stats.pages} />
            </div>
          </div>
        )}
      </div>
    </>
  );
}