"use client";
import { FaCamera, FaSearch } from "react-icons/fa";
import { BiSolidMicrophone } from "react-icons/bi";
import useSearchStore from "@/store/searchStore";
import { useRouter, useSearchParams } from "next/navigation";
import { twMerge } from "tailwind-merge";
import { useEffect } from "react";
import { useDebouncedCallback } from "use-debounce";

export function SearchBar({
  variant = "home",
  height = "h-12",
  width = "w-full",
  textSize = "text-base",
  iconSize = 5,
  ...props
}) {
  const { query, setQuery } = useSearchStore();
  const searchParams = useSearchParams();
  const { push } = useRouter();

  const onSearch = () => {
    if (!query) return;
    const params = new URLSearchParams(searchParams);
    query ? params.set("q", query) : params.delete("q");
    push(`/search?${params.toString()}`);
  };

  // Used to show search suggestion to the user...
  const handleShowSuggestion = useDebouncedCallback((term) => {
    console.log(`Searching... ${term}`);

    const params = new URLSearchParams(searchParams);
    if (term) {
      params.set("query", term);
    } else {
      params.delete("query");
    }
    replace(`${pathname}?${params.toString()}`);
  }, 300);

  const handleSearch = (e) => {
    if (e.key === "Enter") onSearch();
  };

  useEffect(() => {
    if (!query) setQuery(searchParams.get("q"));
  }, []);

  const iconSizeClasses = `w-${iconSize} h-${iconSize}`;

  return (
    <div
      className={twMerge(
        "relative flex flex-row items-center bg-foreground rounded-2xl transition-all duration-300",
        height,
        width,
        props.className
      )}
    >
      {/* Left Icon: Bulb (Always Visible, Fades & Moves in "search" Mode) */}
      <div
        className={twMerge(
          "absolute left-3 flex items-center transition-all duration-300",
          variant === "minimized"
            ? "opacity-70 translate scale-75"
            : "opacity-100"
        )}
      >
        <span className="text-2xl">💡</span>
      </div>

      {/* Input Field */}
      <input
        type="text"
        value={query || ""}
        onChange={(event) => setQuery(event.target.value)}
        onKeyDown={handleSearch}
        className={twMerge(
          "w-full h-full rounded-2xl outline-none text-black bg-foreground font-medium transition-all duration-300",
          variant === "home"
            ? "pl-10 pr-16"
            : variant === "search"
              ? `pl-10 pr-26`
              : `pl-10 pr-24`,
          textSize
        )}
      />

      {/* Right Icons (Always Visible, Shrinks in Minimized Mode) */}
      <div className="absolute right-3 flex items-center space-x-2 transition-all duration-300">
        <BiSolidMicrophone
          className={twMerge(
            "text-gray-500 cursor-pointer transition-all duration-300",
            variant === "minimized"
              ? "scale-75 opacity-70"
              : "scale-100 opacity-100",
            iconSizeClasses
          )}
        />
        <FaCamera
          className={twMerge(
            "text-gray-500 cursor-pointer transition-all duration-300",
            variant === "minimized"
              ? "scale-75 opacity-70"
              : "scale-100 opacity-100",
            iconSizeClasses
          )}
        />
        <button
          onClick={onSearch}
          className={twMerge(
            "hover:bg-gray-200 p-1 rounded-full transition-all duration-300",
            variant === "minimized"
              ? "scale-75 opacity-70"
              : "scale-100 opacity-100"
          )}
        >
          <FaSearch className={`h-4 w-4 text-gray-500 cursor-pointer`} />
        </button>
      </div>
    </div>
  );
}
