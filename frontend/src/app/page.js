"use client";
import { HomeSearchBar } from "@/components/homeSearchBar";
import { SearchButtons } from "@/components/searchButtons";
import useSearchStore from "@/store/searchStore";
import { useEffect } from "react";

export default function Home() {
  const { setQuery } = useSearchStore();

  useEffect(() => {
    setQuery("");
  }, []);

  return (
    <main className="flex items-center justify-center h-screen">
      <div className="flex flex-col w-screen m-auto text-center items-center space-y-6">
        {/* Lumos Logo */}
        <div className="flex flex-row text-7xl text-center items-center">
          <span
            style={{ color: "#CBC5EA" }} // Modern Indigo
            className="font-[Ubuntu] font-[300] tracking-widest"
          >
            L
          </span>
          <span
            style={{ color: "#CBC5EA" }} // Modern Pink
            className="font-[Ubuntu] font-[300] tracking-widest"
          >
            u
          </span>
          <span
            style={{ color: "#E54F6D" }} // Modern Amber
            className="font-[Ubuntu] font-[300] tracking-widest"
          >
            m
          </span>
          <span
            style={{ color: "#E54F6D" }} // Modern Emerald
            className="font-[Ubuntu] font-[300] tracking-widest"
          >
            o
          </span>
          <span
            style={{ color: "#E54F6D" }} // Modern Purple
            className="font-[Ubuntu] font-[300] tracking-widest"
          >
            s
          </span>
          <div className="text-7xl flex items-center">💡</div>
        </div>
        {/* Search Bar */}
        <HomeSearchBar className="flex justify-center h-12 md:w-[40vw] w-[20.5em] px-4" />
        {/* Search Buttons */}
        <SearchButtons />
      </div>
    </main>
  );
}
