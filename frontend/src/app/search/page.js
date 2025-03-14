"use client";
import { redirect, useSearchParams } from "next/navigation";
import { TopBar } from "@/components/topBar";
import useSWR from "swr";
import Image from "next/image";
import { useEffect, useState } from "react";
import { ChevronRight } from "lucide-react";
import { twMerge } from "tailwind-merge";

const fetcher = (...args) => fetch(...args).then((res) => res.json());

function SearchResults({ data, ...props }) {
  // Format URL as breadcrumb pieces
  const formatBreadcrumbParts = (url) => {
    try {
      const urlObj = new URL(url);
      const domain = urlObj.hostname;
      const path = urlObj.pathname.split("/").filter(Boolean);
      const queryParams = urlObj.search
        ? urlObj.search
            .substring(1)
            .split("&")
            .map((param) => param.replace("=", ": "))
        : [];

      const parts = [domain, ...path, ...queryParams];
      return parts;
    } catch (e) {
      return [url]; // Return original URL if parsing fails
    }
  };

  return (
    <div className={twMerge("flex flex-col gap-6", props.className)}>
      {data.map((result, index) => (
        <div key={index} className="flex flex-col">
          {/* URL and favicon */}
          <div className="flex items-center mb-1">
            <div className="flex items-center">
              <div className="flex border border-gray-200 rounded-full w-8 h-8 justify-center items-center overflow-hidden mr-2">
                <Image
                  src="/vercel.svg"
                  alt={result.title}
                  width={20}
                  height={20}
                />
              </div>

              <div className="flex flex-col">
                <div className="text-xl text-white">
                  <span className="font-semibold">
                    {result.url.split(".")[1]}
                  </span>
                </div>
                <div className="text-md text-gray-400 flex items-center">
                  {formatBreadcrumbParts(result.url).map((part, idx, arr) => (
                    <span key={idx} className="flex items-center">
                      {part}
                      {idx < arr.length - 1 && (
                        <ChevronRight
                          size={10}
                          className={"text-gray-400 mx-2"}
                        />
                      )}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Title */}
          <a
            href={result.url}
            className="text-xl text-[#75acff] font-medium hover:underline mb-1 leading-tight"
          >
            {result.title}
          </a>

          {/* Description */}
          <div className="text-md text-white leading-snug max-w-[60vw]">
            {result.description}
          </div>
        </div>
      ))}
    </div>
  );
}

export default function Page() {
  const searchParams = useSearchParams();
  if (!searchParams.has("q") || searchParams.get("q").trim() === "")
    redirect("/");

  const { data, error, isLoading } = useSWR(
    `/api/query?${searchParams.toString()}`,
    fetcher
  );

  // Add state to track scroll position
  const [scrolled, setScrolled] = useState(false);

  // Add scroll event listener
  useEffect(() => {
    const handleScroll = () => {
      // Set scrolled state based on scroll position
      setScrolled(window.scrollY > 50);
    };

    // Add event listener
    window.addEventListener("scroll", handleScroll);

    // Clean up
    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  return (
    <div className="flex flex-col space-y-6 min-h-screen">
      {/* Logo and search bar */}
      <div
        className={`sticky top-0 bg-background border-gray-200 z-10 transition-all duration-300 ${
          scrolled ? "pt-1" : "pt-2"
        }`}
      >
        <TopBar
          className={`mx-14 items-center transition-all duration-300`}
          variant="search"
        />

        <div
          className={`border-b-[1px] mt-2   transition-all duration-300   ${
            scrolled ? "-translate-y-1" : ""
          }`}
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
        <SearchResults data={data} className={"mx-14 w-[50%]"} />
      )}
    </div>
  );
}
