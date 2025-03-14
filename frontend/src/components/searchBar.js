"use client";
import { FaCamera, FaSearch } from "react-icons/fa";
import { BiSolidMicrophone } from "react-icons/bi";
import useSearchStore from "@/store/searchStore";
import { useRouter, useSearchParams } from "next/navigation";
import { twMerge } from "tailwind-merge";
import { useEffect } from "react";
import { X } from "lucide-react";

export function SearchBar({
  variant = "home",
  height = "h-12",
  width = "w-full",
  textSize = "text-base", // Default text size using Tailwind classes
  iconSize = 5, // Default icon size (will be used as w-5 h-5)
  ...props
}) {
  const { query, setQuery } = useSearchStore();
  const searchParams = useSearchParams();
  const { push } = useRouter();

  const onSearch = () => {
    if (!!!query) return;
    const params = new URLSearchParams(searchParams);
    if (query) {
      params.set("q", query);
    } else {
      params.delete("q");
    }
    push(`/search?${params.toString()}`);
  };

  const handleSearch = (e) => {
    if (e.key === "Enter" && onSearch) {
      onSearch();
    }
  };

  useEffect(() => {
    if (!!!query) setQuery(searchParams.get("q"));
  }, []);

  // Compute icon size classes based on the provided iconSize
  const iconSizeClasses = `w-${iconSize} h-${iconSize}`;

  return (
    <div
      className={twMerge(
        "relative flex flex-row items-center bg-foreground rounded-2xl",
        height,
        width,
        props.className
      )}
    >
      {/* Search Icon conditionally rendered based on variant */}
      {variant === "home" && (
        <div className="absolute left-3">
          <FaSearch className={twMerge("text-gray-500", iconSizeClasses)} />
        </div>
      )}

      {/* Input Field - adjust padding based on variant */}
      <input
        type="text"
        value={query || ""}
        placeholder="Search"
        onChange={(event) => (
          setQuery(event.target.value), console.log(event.target.value)
        )}
        onKeyDown={handleSearch}
        className={twMerge(
          "w-full h-full rounded-2xl outline-none text-black bg-foreground font-medium",
          variant === "home" ? "pl-10 pr-16" : "pl-4 pr-35",
          textSize
        )}
      />

      {/* Right Icons - Microphone, Camera & conditionally Search */}
      <div className="absolute right-3 flex items-center space-x-2">
        {variant === "search" && query && (
          <>
            <button
              onClick={() => setQuery("")}
              className="hover:bg-gray-200 p-1 rounded-full"
            >
              <X
                className={`${iconSizeClasses} text-gray-500 cursor-pointer`}
              />
            </button>
            <div
              className={"border-l-2 h-5 w-1 rounded-2xl border-gray-400/90"}
            />
          </>
        )}

        <BiSolidMicrophone
          className={twMerge("text-gray-500 cursor-pointer", iconSizeClasses)}
        />
        <FaCamera
          className={twMerge("text-gray-500 cursor-pointer", iconSizeClasses)}
        />

        {variant === "search" && (
          <>
            <button
              onClick={onSearch}
              className="hover:bg-gray-200 p-1 rounded-full"
            >
              <FaSearch className={`h-4 w-4 text-gray-500 cursor-pointer`} />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
