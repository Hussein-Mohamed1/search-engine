"use client";
import { SearchBar } from "@/components/searchBar";
import Link from "next/link";

export function TopBar({ variant = "home", className }) {
  return (
    <div className={`flex items-center ${className}`}>
      {/* Lumos Logo - Using Nunito font from font variables */}
      <Link href="/" className="mr-8">
        <div className="flex flex-row items-center">
          <div className="font-nunito tracking-widest text-2xl font-extrabold">
            Lumos
          </div>
          <div className="text-xl">💡</div>
        </div>
      </Link>

      {/* Search Bar - uses other fonts from your global settings */}
      <SearchBar variant={variant} width="w-[40em]" height="h-[2.5em]" />
    </div>
  );
}
