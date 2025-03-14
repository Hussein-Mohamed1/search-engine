"use client";
import { useState, useEffect, useRef } from "react";
import { FaCamera, FaSearch } from "react-icons/fa";
import { BiSolidMicrophone } from "react-icons/bi";
import useSearchStore from "@/store/searchStore";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { twMerge } from "tailwind-merge";
import { useDebouncedCallback } from "use-debounce";
const MAX_SUGGESTIONS_COUNT = 5;
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
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const inputRef = useRef(null);
  const suggestionsRef = useRef(null);

  const onSearch = () => {
    if (!query) return;
    const params = new URLSearchParams(searchParams);
    query ? params.set("q", query) : params.delete("q");
    push(`/search?${params.toString()}`);
    setShowSuggestions(false);
  };

  // Fetch suggestions when the query changes
  const fetchSuggestions = useDebouncedCallback(async (term) => {
    if (!term || term.trim().length === 0) {
      setSuggestions([]);
      return;
    }

    try {
      const response = await fetch(
        `/api/suggest?q=${encodeURIComponent(term)}`
      );
      if (response.ok) {
        const data = await response.json();
        setSuggestions(data.slice(0, MAX_SUGGESTIONS_COUNT));
      } else {
        console.error("Failed to fetch suggestions");
        setSuggestions([]);
      }
    } catch (error) {
      console.error("Error fetching suggestions:", error);
      setSuggestions([]);
    }
  }, 300);

  const handleSearch = (e) => {
    if (e.key === "Enter") onSearch();
  };

  const handleInputChange = (event) => {
    const newQuery = event.target.value;
    setQuery(newQuery);
    fetchSuggestions(newQuery);
    setShowSuggestions(true);
  };

  const handleSuggestionClick = (suggestion) => {
    setQuery(suggestion);
    setShowSuggestions(false);
    // Trigger search with the selected suggestion
    const params = new URLSearchParams(searchParams);
    params.set("q", suggestion);
    push(`/search?${params.toString()}`);
  };

  // Close suggestions when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target) &&
        !inputRef.current.contains(event.target)
      ) {
        setShowSuggestions(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // Hide the suggestions if the search bar is minimized
  useEffect(() => {
    if (variant === "minimized") setShowSuggestions(false);
  }, [variant]);

  useEffect(() => {
    if (!query) setQuery(searchParams.get("q") || "");
  }, []);

  const iconSizeClasses = `w-${iconSize} h-${iconSize}`;

  return (
    <div className="relative">
      <div
        className={twMerge(
          "relative flex flex-row items-center bg-foreground rounded-2xl transition-all duration-300",
          height,
          width,
          props.className
        )}
      >
        {/* Left Icon: Bulb (Always Visible, Fades & Moves in "search" Mode) */}
        {variant !== "home" && (
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
        )}
        {/* Input Field */}
        <input
          ref={inputRef}
          type="text"
          value={query || ""}
          onChange={handleInputChange}
          onKeyDown={handleSearch}
          onFocus={() => query && setShowSuggestions(true)}
          className={twMerge(
            "w-full h-full rounded-2xl outline-none text-black bg-foreground font-medium transition-all duration-300",
            variant === "home"
              ? "pl-4 pr-22"
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

      {/* Suggestions Dropdown */}
      {showSuggestions && suggestions.length > 0 && (
        <div
          ref={suggestionsRef}
          className="absolute z-10 mt-1 w-full bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden"
        >
          <ul className="py-1">
            {suggestions.map((suggestion, index) => (
              <li
                key={index}
                className="px-4 py-2 text-black hover:bg-gray-100 cursor-pointer flex items-center"
                onClick={() => handleSuggestionClick(suggestion)}
              >
                <FaSearch className="h-3 w-3 text-gray-400 mr-3" />
                <span>{suggestion}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
